import asyncio
import json
import logging
import os

import httpx
import redis.asyncio as redis

from app.documents.service import DocumentService

logger = logging.getLogger(__name__)

# Define queue names for clarity
INDEXING_QUEUE_KEY = "document-indexing-queue"
DEINDEXING_QUEUE_KEY = "document-deindexing-queue"


async def update_status(base_url: str, document_id: int, status: str):
    """Updates the indexing status of a document via a call to the core-api."""
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


async def run_job_consumer(redis_client: redis.Redis, document_service: DocumentService):
    """Continuously consumes and processes document jobs (indexing and de-indexing) from Redis queues."""
    logger.info(f"Starting job consumer, listening on queues: {[INDEXING_QUEUE_KEY, DEINDEXING_QUEUE_KEY]}")
    core_api_base_url = os.getenv("CORE_API_BASE_URL", "http://core-api:8080")

    while True:
        queue = None
        job_json = None
        try:
            # Listen to both queues and get the job and the queue it came from
            queue, job_json = await redis_client.brpop([INDEXING_QUEUE_KEY, DEINDEXING_QUEUE_KEY])
            if job_json is None:
                continue

            job_data = json.loads(job_json)
            doc_id = job_data['documentId']

            # --- Process Indexing Job ---
            if queue == INDEXING_QUEUE_KEY:
                stored_name = job_data['storedName']
                original_filename = job_data['originalFilename']
                category = job_data.get('category')

                logger.info(f"Received INDEXING job for doc_id: {doc_id} (filename: {original_filename})")
                file_path = f"/app/documents/{stored_name}"

                try:
                    await asyncio.to_thread(
                        document_service.add_document,
                        file_path, doc_id, original_filename, category
                    )
                    logger.info(f"Successfully indexed doc_id: {doc_id}")
                    await update_status(core_api_base_url, doc_id, "SUCCESS")
                except Exception as e:
                    logger.error(f"Failed to index doc_id: {doc_id}. Error: {e}", exc_info=True)
                    await update_status(core_api_base_url, doc_id, "FAILURE")

            # --- Process De-indexing Job ---
            elif queue == DEINDEXING_QUEUE_KEY:
                logger.info(f"Received DE-INDEXING job for doc_id: {doc_id}")
                try:
                    await asyncio.to_thread(document_service.delete_document, doc_id)
                    logger.info(f"Successfully de-indexed doc_id: {doc_id}")
                except Exception as e:
                    logger.error(f"Failed to de-index doc_id: {doc_id}. Error: {e}", exc_info=True)

        except redis.ConnectionError as e:
            logger.error(f"Redis connection error: {e}. Retrying in 5 seconds...")
            await asyncio.sleep(5)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to decode job JSON: {job_json}. Error: {e}")
        except Exception as e:
            logger.error(f"An unexpected error occurred in the consumer: {e}", exc_info=True)
            await asyncio.sleep(5)