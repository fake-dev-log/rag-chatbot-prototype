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
        es_host = os.environ.get("ELASTICSEARCH_HOST", "elasticsearch")
        es_password = os.environ.get("ELASTICSEARCH_PASSWORD")

        es_url = f"http://{es_host}:9200"

        logger.info(f"Initializing ElasticsearchStore for retriever with URL: {es_url} and index: {INDEX_NAME}")
        try:
            self.vector_store = ElasticsearchStore(
                es_url=es_url,
                index_name=INDEX_NAME,
                strategy=DenseVectorStrategy(),
                embedding=hugging_face.get_embedding(),
                es_user='elastic',
                es_password=es_password
            )
            logger.info("ElasticsearchStore connection initialized successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize ElasticsearchStore: {e}")
            raise

    def get_vector_store(self) -> ElasticsearchStore:
        """Returns the configured ElasticsearchStore instance."""
        return self.vector_store
