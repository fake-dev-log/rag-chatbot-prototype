import logging
import os
from pathlib import PurePath, Path
from typing import Union

from langchain_community.vectorstores import FAISS
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from ..embedding import hugging_face

VECTOR_STORE_DIR = '/app/vector-store'

logger = logging.getLogger(__name__)


def load_pdf(pdf_path: Union[str, PurePath]) -> list[Document]:
    """Loads a PDF and returns a list of Document objects."""
    loader = PyPDFLoader(str(pdf_path))
    return loader.load()


def split_text(doc: list[Document]) -> list[Document]:
    """Splits a list of Documents into smaller chunks."""
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        length_function=len
    )
    return text_splitter.split_documents(doc)


class DataProcessor:
    vector_store_path = VECTOR_STORE_DIR

    def __init__(self):
        """
        Initializes the DataProcessor by loading or creating the vector store.
        """
        self.embedding = hugging_face.get_embedding()
        self.vector_store = self._load_or_initialize_vector_store()

    def _load_or_initialize_vector_store(self) -> FAISS:
        """
        Loads the FAISS index from disk if it exists, otherwise initializes a new one.
        """
        # LangChain FAISS index uses pickle, which can be insecure.
        # Set allow_dangerous_deserialization=True to load the index.
        if Path(self.vector_store_path).exists() and os.listdir(self.vector_store_path):
            logger.info(f"Loading existing FAISS index from {self.vector_store_path}")
            return FAISS.load_local(self.vector_store_path, self.embedding, allow_dangerous_deserialization=True)
        else:
            logger.info("No existing FAISS index found. Initializing a new one.")
            os.makedirs(self.vector_store_path, exist_ok=True)
            # FAISS must be initialized with some documents to set the embedding dimension.
            dummy_doc = [Document(page_content="init")]
            vector_store = FAISS.from_documents(dummy_doc, self.embedding)
            vector_store.save_local(self.vector_store_path)
            return vector_store

    def add_document(self, pdf_path: Union[str, PurePath]) -> None:
        """
        Processes a single PDF, adds it to the vector store, and saves the index.
        :param pdf_path: Path to the PDF file.
        """
        pdf_path = Path(pdf_path)
        logger.info(f"Processing and adding document: {pdf_path.name}")
        doc = load_pdf(pdf_path)
        chunks = split_text(doc)

        # Add file_name to metadata for each chunk to enable targeted deletion
        for chunk in chunks:
            chunk.metadata["file_name"] = pdf_path.name

        self.vector_store.add_documents(chunks)
        self.vector_store.save_local(self.vector_store_path)
        logger.info(f"Successfully added {pdf_path.name} to the vector store.")

    def delete_document(self, file_name: str) -> bool:
        """
        Deletes all vectors associated with a specific file_name from the vector store.
        Note: This is an expensive operation as it requires iterating through the docstore.
        :param file_name: The name of the file to delete.
        :return: True if documents were deleted, False otherwise.
        """
        if not self.vector_store:
            logger.error("Vector store is not initialized.")
            return False

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
        Updates a document in the vector store by deleting the old version and adding the new one.
        :param pdf_path: Path to the new version of the PDF file.
        """
        pdf_path = Path(pdf_path)
        file_name = pdf_path.name
        logger.info(f"Updating document: {file_name}")
        self.delete_document(file_name)
        self.add_document(pdf_path)
        logger.info(f"Successfully updated document: {file_name}")
