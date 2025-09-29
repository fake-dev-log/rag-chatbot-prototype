import logging
import os

from langchain_elasticsearch import ElasticsearchStore, DenseVectorStrategy
from app.modules.embedding import hugging_face

logger = logging.getLogger(__name__)

INDEX_NAME = "rag_documents"


class RetrieverProvider:
    """
    Manages the connection to the Elasticsearch vector store.
    """
    def __init__(self):
        """Initializes the ElasticsearchStore."""
        embedding = hugging_face.get_embedding()
        es_url = os.environ.get("ELASTICSEARCH_URL", "http://elasticsearch:9200")
        logger.info(f"Initializing ElasticsearchStore for retriever with URL: {es_url} and index: {INDEX_NAME}")

        mapping = {
            "properties": {
                "metadata": {
                    "properties": {
                        "category": {"type": "keyword"},
                        "file_name": {"type": "keyword"},
                        "page_number": {"type": "integer"}
                    }
                }
            }
        }

        self.vector_store = ElasticsearchStore(
            es_url=es_url,
            index_name=INDEX_NAME,
            embedding=embedding,
            custom_index_settings={"mappings": mapping}, # Use correct parameter
            strategy=DenseVectorStrategy()
        )
        logger.info("ElasticsearchStore connection initialized successfully.")

    def get_vector_store(self) -> ElasticsearchStore:
        """Returns the configured ElasticsearchStore instance."""
        return self.vector_store
