import asyncio
import logging
import os
from contextlib import asynccontextmanager
from logging.config import dictConfig

import redis.asyncio as redis
from fastapi import FastAPI

import uvicorn

from .documents.service import DocumentService
from .extensions.logging_config import LOGGING_CONFIG
from .jobs.consumer import run_job_consumer

dictConfig(LOGGING_CONFIG)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manages the application's lifespan, starting and stopping background tasks."""
    # Initialize services and clients
    redis_host = os.getenv("REDIS_HOST", "redis")
    redis_client = redis.Redis(host=redis_host, port=6379, decode_responses=True)
    document_service = DocumentService()

    # Start the background consumer task from the dedicated module
    consumer_task = asyncio.create_task(run_job_consumer(redis_client, document_service))

    yield

    # Clean up: cancel the consumer task upon application shutdown
    logger.info("Shutting down... Cancelling background tasks.")
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        logger.info("Job consumer task successfully cancelled.")


app = FastAPI(lifespan=lifespan)


@app.get("/health", tags=["Health"])
async def health_check():
    """Performs a health check of the service."""
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8001)