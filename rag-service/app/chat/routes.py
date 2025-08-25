from http import HTTPStatus

from fastapi import APIRouter, Depends, HTTPException
from starlette.responses import StreamingResponse, Response

from app.dependencies import verify_api_key
from .schemas import ChatRequest
from .service import ChatService

router = APIRouter(prefix='/chats')
service = ChatService()


@router.head('', dependencies=[Depends(verify_api_key)])
async def readiness_check():
    if service.ready:
        return Response(status_code=HTTPStatus.OK)
    else:
        # HTTP 503: Service Unavailable
        return HTTPException(status_code=503)


@router.post(path='', dependencies=[Depends(verify_api_key)])
async def chat(request: ChatRequest):
    return StreamingResponse(service.stream(request.query), media_type="application/x-ndjson")


