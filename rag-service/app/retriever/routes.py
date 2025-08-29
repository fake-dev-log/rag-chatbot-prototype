from http import HTTPStatus

from fastapi import APIRouter, Depends, Response

from app.chat.service import ChatService
from app.dependencies import verify_api_key

router = APIRouter(prefix='/retriever', dependencies=[Depends(verify_api_key)])


def get_chat_service() -> ChatService:
    # This function can be extended to manage ChatService lifecycle,
    # e.g., using a global singleton pattern or caching.
    return ChatService()


@router.post("/reload", summary="Reload the retriever's vector store index")
async def reload_retriever(
    chat_service: ChatService = Depends(get_chat_service)
):
    """
    Triggers a reload of the FAISS vector store from disk.
    This is useful after the indexing service has updated the index.
    """
    try:
        chat_service.reload_retriever()
        return Response(status_code=HTTPStatus.NO_CONTENT)
    except Exception as e:
        # A more specific exception handling would be better here
        return Response(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, content=f"Failed to reload retriever: {e}")
