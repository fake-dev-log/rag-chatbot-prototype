import logging
import os
from pathlib import PurePath, Path
from typing import Union

from langchain_community.vectorstores import FAISS
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from ..embedding import hugging_face

# Define the directory where the vector store will be saved.
VECTOR_STORE_DIR = '/app/vector-store'

logger = logging.getLogger(__name__)


def load_pdf(pdf_path: Union[str, PurePath]) -> list[Document]:
    """Loads a PDF from the given path and returns its content as a list of Document objects."""
    loader = PyPDFLoader(str(pdf_path))
    return loader.load()


def split_text(doc: list[Document]) -> list[Document]:
    """Splits a list of Document objects into smaller chunks for efficient processing."""
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,       # The maximum size of each chunk (in characters).
        chunk_overlap=200,     # The number of characters to overlap between chunks.
        length_function=len
    )
    return text_splitter.split_documents(doc)


class DataProcessor:
    """
    Handles all data processing tasks, including loading documents, splitting text,
    and managing the FAISS vector store for retrieval.
    """
    vector_store_path = VECTOR_STORE_DIR

    def __init__(self):
        """
        Initializes the DataProcessor, loading the embedding model and the vector store.
        """
        self.embedding = hugging_face.get_embedding()
        self.vector_store = self._load_or_initialize_vector_store()

    def _load_or_initialize_vector_store(self) -> FAISS:
        """
        Loads an existing FAISS index from disk. If it doesn't exist, creates a new one.
        """
        # Check if a vector store already exists at the specified path.
        if Path(self.vector_store_path).exists() and os.listdir(self.vector_store_path):
            logger.info(f"Loading existing FAISS index from {self.vector_store_path}")
            # Note: LangChain's FAISS implementation uses pickle for serialization.
            # Loading a pickled file can be a security risk, so `allow_dangerous_deserialization`
            # must be explicitly set to True.
            return FAISS.load_local(self.vector_store_path, self.embedding, allow_dangerous_deserialization=True)
        else:
            logger.info("No existing FAISS index found. Initializing a new one.")
            os.makedirs(self.vector_store_path, exist_ok=True)
            # A FAISS index must be initialized with at least one document to establish
            # the correct embedding dimension. A dummy document is used for this purpose.
            dummy_doc = [Document(page_content="init")]
            vector_store = FAISS.from_documents(dummy_doc, self.embedding)
            vector_store.save_local(self.vector_store_path)
            return vector_store

    def add_document(self, pdf_path: Union[str, PurePath]) -> None:
        """
        Processes and adds a single PDF document to the vector store.

        Args:
            pdf_path: The path to the PDF file to be added.
        """
        pdf_path = Path(pdf_path)
        logger.info(f"Processing and adding document: {pdf_path.name}")
        doc = load_pdf(pdf_path)
        chunks = split_text(doc)

        # Tag each chunk with the source file name. This is crucial for enabling
        # targeted deletion of documents from the vector store later.
        for chunk in chunks:
            chunk.metadata["file_name"] = pdf_path.name

        self.vector_store.add_documents(chunks)
        self.vector_store.save_local(self.vector_store_path)
        logger.info(f"Successfully added {pdf_path.name} to the vector store.")

    def delete_document(self, file_name: str) -> bool:
        """
        Deletes all vectors associated with a specific file name from the vector store.

        Note: This is a potentially expensive operation as it requires a linear scan
        through the entire document store to find matching IDs.

        Args:
            file_name: The name of the file to delete.

        Returns:
            True if documents were deleted, False otherwise.
        """
        if not self.vector_store:
            logger.error("Vector store is not initialized.")
            return False

        # Find the internal FAISS IDs for all chunks belonging to the specified file.
        ids_to_delete = [
            doc_id for doc_id, doc in self.vector_store.docstore._dict.items()
            if doc.metadata.get("file_name") == file_name
        ]

        if not ids_to_delete:
            logger.warning(f"No documents found with file_name: {file_name}. Nothing to delete.")
            return False

        logger.info(f"Deleting {len(ids_to_delete)} vectors for file: {file_name}")
        self.vector_store.delete(ids_to_delete)
        self.vector_store.save_local(self.vector_store_path)
        logger.info(f"Successfully deleted vectors for {file_name}.")
        return True

    def update_document(self, pdf_path: Union[str, PurePath]) -> None:
        """
        Updates a document in the vector store by performing a delete-then-add operation.

        Args:
            pdf_path: The path to the new version of the PDF file.
        """
        pdf_path = Path(pdf_path)
        file_name = pdf_path.name
        logger.info(f"Updating document: {file_name}")
        # First, delete all existing vectors associated with the document.
        self.delete_document(file_name)
        # Then, add the new version of the document.
        self.add_document(pdf_path)
        logger.info(f"Successfully updated document: {file_name}")