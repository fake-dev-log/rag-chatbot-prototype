import logging
import os
import uuid
from http import HTTPStatus

import httpx
import redis
from fastapi import APIRouter, Depends, HTTPException

from app.dependencies import verify_api_key
from app.modules.data.processor import DataProcessor
from .schemas import DocumentRequest, IndexingResponse, DeleteResponse

# --- Service Communication Setup ---
# RAG Service URL using Docker's internal network alias
RAG_SERVICE_URL = "http://rag-service:8000"

# Redis client for one-time API keys
REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)
# -------------------------------------

logger = logging.getLogger(__name__)
router = APIRouter(prefix='/documents', dependencies=[Depends(verify_api_key)])
data_processor = DataProcessor()


async def trigger_rag_retriever_reload():
    """
    Notifies the RAG service to reload its retriever using a one-time API key.
    """
    # As per the reference implementation, generate a one-time key/secret pair
    key = str(uuid.uuid4())
    secret = str(uuid.uuid4())
    try:
        # Store the key-secret pair in Redis with a short expiration (e.g., 10 seconds)
        redis_client.set(key, secret, ex=10)
    except redis.exceptions.ConnectionError as e:
        logger.error(f"Could not connect to Redis to set one-time key: {e}")
        # Fail fast if we can't contact Redis, as the subsequent request will be denied.
        raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail="Could not connect to Redis.")

    reload_url = f"{RAG_SERVICE_URL}/retriever/reload"
    headers = {"X-API-KEY": key, "X-API-SECRET": secret}

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(reload_url, headers=headers)
            response.raise_for_status()
            logger.info("Successfully triggered RAG service retriever reload.")
    except httpx.RequestError as e:
        logger.error(f"Failed to trigger RAG service reload: {e}")
        # This failure should be handled (e.g., by a retry mechanism or monitoring)


@router.post(
    "",
    response_model=IndexingResponse,
    summary="Add a new document to the vector store"
)
async def add_document(request: DocumentRequest):
    try:
        data_processor.add_document(request.file_path)
        await trigger_rag_retriever_reload()
        return {"file_name": os.path.basename(request.file_path), "detail": "Document added successfully."}
    except FileNotFoundError:
        raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {request.file_path}")
    except Exception as e:
        logger.error(f"Error adding document {request.file_path}: {e}")
        raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))


@router.put(
    "",
    response_model=IndexingResponse,
    summary="Update an existing document in the vector store"
)
async def update_document(request: DocumentRequest):
    try:
        data_processor.update_document(request.file_path)
        await trigger_rag_retriever_reload()
        return {"file_name": os.path.basename(request.file_path), "detail": "Document updated successfully."}
    except FileNotFoundError:
        raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {request.file_path}")
    except Exception as e:
        logger.error(f"Error updating document {request.file_path}: {e}")
        raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))


@router.delete(
    "/{file_name}",
    response_model=DeleteResponse,
    summary="Delete a document from the vector store"
)
async def delete_document(file_name: str):
    try:
        deleted = data_processor.delete_document(file_name)
        if deleted:
            await trigger_rag_retriever_reload()
            return {"file_name": file_name, "is_deleted": True, "detail": "Document deleted successfully."}
        else:
            return {"file_name": file_name, "is_deleted": False, "detail": "Document not found or already deleted."}
    except Exception as e:
        logger.error(f"Error deleting document {file_name}: {e}")
        raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))
