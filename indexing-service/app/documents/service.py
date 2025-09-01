import logging
import os
import uuid
from http import HTTPStatus

import httpx
import redis
from fastapi import HTTPException

from app.modules.data.processor import DataProcessor


logger = logging.getLogger(__name__)


class DocumentService:
    """
    Handles the business logic for document indexing, including adding, updating,
    and deleting documents in the vector store. It also coordinates with the
    RAG service to ensure the retriever is using the most up-to-date index.
    """

    def __init__(self):
        """Initializes the service, setting up connections and the data processor."""
        # The URL for the RAG service, using Docker's internal network alias.
        self.RAG_SERVICE_URL = "http://rag-service:8000"

        # Initialize Redis client for inter-service communication via one-time keys.
        self.REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
        self.REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
        self.redis_client = redis.Redis(host=self.REDIS_HOST, port=self.REDIS_PORT, db=0, decode_responses=True)

        # The DataProcessor handles the actual document processing and vector store interaction.
        self.data_processor = DataProcessor()

    async def trigger_rag_retriever_reload(self):
        """
        Notifies the RAG service to reload its vector store retriever.

        This is a crucial step after any modification to the vector store (add, update, delete)
        to ensure the RAG service uses the latest data. It uses a secure, one-time
        API key mechanism for the request.
        """
        # Generate a unique, single-use key/secret pair for this request.
        key = str(uuid.uuid4())
        secret = str(uuid.uuid4())
        try:
            # Store the key in Redis with a short time-to-live (TTL) to prevent reuse.
            self.redis_client.set(key, secret, ex=10)
        except redis.exceptions.ConnectionError as e:
            logger.error(f"Could not connect to Redis to set one-time key: {e}")
            # If Redis is down, the request to the RAG service will fail authentication.
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail="Could not connect to Redis.")

        reload_url = f"{self.RAG_SERVICE_URL}/retriever/reload"
        headers = {"X-API-KEY": key, "X-API-SECRET": secret}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(reload_url, headers=headers)
                response.raise_for_status()  # Raise an exception for 4xx or 5xx status codes
                logger.info("Successfully triggered RAG service retriever reload.")
        except httpx.RequestError as e:
            logger.error(f"Failed to trigger RAG service reload: {e}")
            # In a production environment, this might trigger a retry mechanism or an alert.

    def add_document(self, file_path: str, document_name: str):
        """Adds a new document to the vector store."""
        try:
            self.data_processor.add_document(file_path)
            return {"file_name": document_name, "detail": "Document added successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error adding document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def update_document(self, file_path: str):
        """Updates an existing document in the vector store."""
        try:
            self.data_processor.update_document(file_path)
            return {"file_name": os.path.basename(file_path), "detail": "Document updated successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error updating document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def delete_document(self, file_name: str):
        """Deletes a document and its corresponding vectors from the store."""
        logger.info(f"Deleting document {file_name}")
        try:
            deleted = self.data_processor.delete_document(file_name)
            if deleted:
                return {"file_name": file_name, "is_deleted": True, "detail": "Document deleted successfully."}
            else:
                return {"file_name": file_name, "is_deleted": False, "detail": "Document not found or already deleted."}
        except Exception as e:
            logger.error(f"Error deleting document {file_name}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))