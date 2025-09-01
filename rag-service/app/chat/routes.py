from http import HTTPStatus

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from starlette.responses import StreamingResponse, Response

from app.dependencies import verify_api_key
from .schemas import ChatRequest
from .service import ChatService

router = APIRouter(prefix='/chats')
# Initialize a singleton instance of the ChatService to be shared across requests.
service = ChatService()


@router.head('', dependencies=[Depends(verify_api_key)])
async def readiness_check():
    """
    Performs a readiness check for the ChatService.

    This endpoint is used to verify if the service is ready to accept requests,
    which is particularly useful for services that have a long startup time
    (e.g., loading large machine learning models).

    Returns:
        - 200 OK: If the service is ready.
        - 503 Service Unavailable: If the service is not yet ready.
    """
    if service.ready:
        return Response(status_code=HTTPStatus.OK)
    else:
        raise HTTPException(status_code=HTTPStatus.SERVICE_UNAVAILABLE, detail="Service is not ready")





@router.post(path='', dependencies=[Depends(verify_api_key)])
async def chat(request: ChatRequest):
    """
    Handles a user's chat query and streams the response.

    Args:
        request: A ChatRequest object containing the user's query.

    Returns:
        A StreamingResponse that sends newline-delimited JSON (NDJSON) objects.
        Each object in the stream represents an event (e.g., a token or source documents).
    """
    return StreamingResponse(service.stream(request.query), media_type="application/x-ndjson")