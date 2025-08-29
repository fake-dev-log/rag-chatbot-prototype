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
    def __init__(self):
        # --- Service Communication Setup ---
        # RAG Service URL using Docker's internal network alias
        self.RAG_SERVICE_URL = "http://rag-service:8000"

        # Redis client for one-time API keys
        self.REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
        self.REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
        self.redis_client = redis.Redis(host=self.REDIS_HOST, port=self.REDIS_PORT, db=0, decode_responses=True)
        # -------------------------------------
        self.data_processor = DataProcessor()

    async def trigger_rag_retriever_reload(self):
        """
        Notifies the RAG service to reload its retriever using a one-time API key.
        """
        # As per the reference implementation, generate a one-time key/secret pair
        key = str(uuid.uuid4())
        secret = str(uuid.uuid4())
        try:
            # Store the key-secret pair in Redis with a short expiration (e.g., 10 seconds)
            self.redis_client.set(key, secret, ex=10)
        except redis.exceptions.ConnectionError as e:
            logger.error(f"Could not connect to Redis to set one-time key: {e}")
            # Fail fast if we can't contact Redis, as the subsequent request will be denied.
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail="Could not connect to Redis.")

        reload_url = f"{self.RAG_SERVICE_URL}/retriever/reload"
        headers = {"X-API-KEY": key, "X-API-SECRET": secret}

        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(reload_url, headers=headers)
                response.raise_for_status()
                logger.info("Successfully triggered RAG service retriever reload.")
        except httpx.RequestError as e:
            logger.error(f"Failed to trigger RAG service reload: {e}")
            # This failure should be handled (e.g., by a retry mechanism or monitoring)

    def add_document(self, file_path: str):
        filename = os.path.basename(file_path)
        try:
            self.data_processor.add_document(file_path)
            return {"file_name": filename, "detail": "Document added successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error adding document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def update_document(self, file_path: str):
        try:
            self.data_processor.update_document(file_path)
            return {"file_name": os.path.basename(file_path), "detail": "Document updated successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error updating document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def delete_document(self, file_name: str):
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
