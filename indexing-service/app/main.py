import asyncio
import json
import logging
import os
from contextlib import asynccontextmanager
from logging.config import dictConfig

import httpx
import redis.asyncio as redis  # Use asyncio version of redis
from fastapi import FastAPI

import uvicorn

from .documents.service import DocumentService
from .extensions.logging_config import LOGGING_CONFIG
from .documents import router as documents_router

dictConfig(LOGGING_CONFIG)
logger = logging.getLogger(__name__)


async def consume_indexing_jobs(redis_client: redis.Redis, document_service: DocumentService):
    logger.info("Starting indexing job consumer...")
    core_api_base_url = os.getenv("CORE_API_BASE_URL", "http://core-api:8080")
    queue_name = "document-indexing-queue"

    while True:
        job_json = None
        try:
            # Asynchronously and blocking pop from the Redis list
            job_tuple = await redis_client.brpop([queue_name])

            if job_tuple is None:
                continue

            _, job_json = job_tuple
            job_data = json.loads(job_json)
            document_id = job_data['documentId']
            stored_name = job_data['storedName']
            category = job_data.get('category')

            logger.info(f"Received indexing job for documentId: {document_id}")

            file_path = f"/app/documents/{stored_name}"

            try:
                # Perform the actual indexing (this is a blocking I/O operation)
                # In a high-performance scenario, this might also be run in a thread
                await asyncio.to_thread(document_service.add_document, file_path, stored_name, category)
                logger.info(f"Successfully indexed documentId: {document_id}")
                # Report success back to core-api
                await update_status(core_api_base_url, document_id, "SUCCESS")
            except Exception as e:
                logger.error(f"Failed to index documentId: {document_id}. Error: {e}")
                # Report failure back to core-api
                await update_status(core_api_base_url, document_id, "FAILURE")

        except redis.ConnectionError as e:
            logger.error(f"Redis connection error: {e}. Retrying in 5 seconds...")
            await asyncio.sleep(5)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to decode job JSON: {job_json}. Error: {e}")
        except Exception as e:
            logger.error(f"An unexpected error occurred in the consumer: {e}")
            await asyncio.sleep(5) # Avoid rapid-fire loops on unexpected errors


async def update_status(base_url: str, document_id: int, status: str):
    async with httpx.AsyncClient() as client:
        try:
            response = await client.patch(
                f"{base_url}/internal/documents/{document_id}/status",
                json={"status": status}
            )
            response.raise_for_status()
            logger.info(f"Successfully updated status to {status} for documentId: {document_id}")
        except httpx.HTTPStatusError as e:
            logger.error(f"Failed to update status for documentId: {document_id}. Status: {e.response.status_code}, Body: {e.response.text}")
        except httpx.RequestError as e:
            logger.error(f"Request error while updating status for documentId: {document_id}. Error: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize services and clients
    redis_host = os.getenv("REDIS_HOST", "redis")
    redis_client = redis.Redis(host=redis_host, port=6379, decode_responses=True)
    document_service = DocumentService()
    
    # Start the background consumer task
    consumer_task = asyncio.create_task(consume_indexing_jobs(redis_client, document_service))
    
    yield
    
    # Clean up: cancel the consumer task
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        logger.info("Indexing job consumer task cancelled.")


app = FastAPI(lifespan=lifespan)

app.include_router(documents_router, tags=["Documents"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)
