from fastapi import APIRouter, Depends
from starlette.responses import StreamingResponse

from app import verify_api_key
from .schemas import ChatRequest
from .service import ChatService

router = APIRouter(prefix='/chats')
service = ChatService()


@router.post(path='', dependencies=[Depends(verify_api_key)])
async def chat(request: ChatRequest):
    return StreamingResponse(service.stream(request.query), media_type="application/x-ndjson")
