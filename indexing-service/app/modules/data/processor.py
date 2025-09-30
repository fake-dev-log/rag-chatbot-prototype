import logging
import os
from pathlib import Path
from typing import Union

from langchain_elasticsearch import ElasticsearchStore, DenseVectorStrategy
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document

from ..embedding import hugging_face

logger = logging.getLogger(__name__)

INDEX_NAME = "rag_documents"


def load_pdf(pdf_path: Union[str, Path]) -> list[Document]:
    """Loads a PDF from the given path and returns its content as a list of Document objects."""
    loader = PyPDFLoader(str(pdf_path))
    return loader.load()


def split_text(doc: list[Document]) -> list[Document]:
    """Splits a list of Document objects into smaller chunks for efficient processing."""
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=1000,
        chunk_overlap=200,
        length_function=len
    )
    return text_splitter.split_documents(doc)


class DataProcessor:
    """
    Handles all data processing tasks, including loading documents, splitting text,
    and managing the Elasticsearch vector store for retrieval.
    """

    def __init__(self):
        """
        Initializes the DataProcessor, loading the embedding model and setting up the Elasticsearch vector store.
        It also ensures that the index exists with the correct mapping.
        """
        es_host = os.environ.get("ELASTICSEARCH_HOST", "elasticsearch")
        es_password = os.environ.get("ELASTICSEARCH_PASSWORD")

        es_url = f"http://{es_host}:9200"

        logger.info(f"Initializing ElasticsearchStore with URL: {es_url} and index: {INDEX_NAME}")
        try:
            self.vector_store = ElasticsearchStore(
                es_url=es_url,
                index_name=INDEX_NAME,
                strategy=DenseVectorStrategy(),
                embedding=hugging_face.get_embedding(),
                es_user='elastic',
                es_password=es_password
            )
            self._create_index_if_not_exists()
            logger.info("ElasticsearchStore initialized successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize ElasticsearchStore or create index: {e}")
            raise

    def _create_index_if_not_exists(self):
        """Creates the Elasticsearch index with the correct mapping if it doesn't exist."""
        client = self.vector_store.client
        if not client.indices.exists(index=INDEX_NAME):
            logger.info(f"Index '{INDEX_NAME}' not found. Creating it with the specified mapping.")
            mapping = {
                "properties": {
                    "metadata": {
                        "properties": {
                            "category": {"type": "keyword"},
                            "file_name": {"type": "keyword"},
                            "page_number": {"type": "integer"}
                        }
                    },
                    "text": {"type": "text"},
                    "vector": {
                        "type": "dense_vector",
                        "dims": 1024,  # Specify the dimension of the embedding vector
                        "index": True,
                        "similarity": "cosine"
                    }
                }
            }
            try:
                client.indices.create(index=INDEX_NAME, mappings=mapping)
                logger.info(f"Index '{INDEX_NAME}' created successfully.")
            except Exception as e:
                logger.error(f"Failed to create index '{INDEX_NAME}': {e}")
                raise
        else:
            logger.debug(f"Index '{INDEX_NAME}' already exists.")

    def add_document(self, pdf_path: Union[str, Path], document_name: str = None, category: str | None = None) -> None:
        """
        Processes and adds a single PDF document to the Elasticsearch index.
        """
        pdf_path = Path(pdf_path)
        logger.info(f"Processing and adding document: {pdf_path.name}")
        doc = load_pdf(pdf_path)
        chunks = split_text(doc)

        if not document_name:
            document_name = pdf_path.name

        # Tag each chunk with metadata
        for chunk in chunks:
            chunk.metadata["file_name"] = document_name
            chunk.metadata["page_number"] = chunk.metadata.get("page", 0) + 1
            if category:
                chunk.metadata["category"] = category

        self.vector_store.add_documents(chunks)
        logger.info(f"Successfully added {pdf_path.name} to the Elasticsearch index.")

    def delete_document(self, file_name: str) -> bool:
        """
        Deletes all vectors associated with a specific file name from Elasticsearch.
        """
        logger.info(f"Deleting document with file_name: {file_name} from Elasticsearch.")
        try:
            self.vector_store.client.delete_by_query(
                index=INDEX_NAME,
                body={
                    "query": {
                        "term": {
                            "metadata.file_name.keyword": file_name
                        }
                    }
                }
            )
            logger.info(f"Successfully submitted deletion request for file: {file_name}")
            return True
        except Exception as e:
            logger.error(f"Error deleting document {file_name} from Elasticsearch: {e}")
            return False

    def update_document(self, pdf_path: Union[str, Path], document_name: str = None, category: str | None = None) -> None:
        """
        Updates a document in Elasticsearch by performing a delete-then-add operation.
        """
        pdf_path = Path(pdf_path)
        file_name_to_delete = document_name if document_name is not None else pdf_path.name
        
        logger.info(f"Updating document: {file_name_to_delete}")
        # First, delete all existing vectors associated with the document.
        self.delete_document(file_name_to_delete)
        # Then, add the new version of the document.
        self.add_document(pdf_path, document_name=file_name_to_delete, category=category)
        logger.info(f"Successfully updated document: {pdf_path.name}")