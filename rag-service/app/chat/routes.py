from http import HTTPStatus

from fastapi import APIRouter, Depends, HTTPException
from starlette.responses import StreamingResponse, Response

from app.dependencies import verify_api_key
from .schemas import ChatRequest
from .service import ChatService

router = APIRouter(prefix='/chats')
# Initialize a singleton instance of the ChatService to be shared across requests.
service = ChatService()


@router.head("", status_code=HTTPStatus.OK)
async def readiness_check():
    """
    Performs a readiness check. Returns 200 OK if the service is running and ready to accept requests.
    """
    return Response()





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
    return StreamingResponse(service.stream(query=request.query, chat_history=request.chat_history, category=request.category), media_type="application/x-ndjson")