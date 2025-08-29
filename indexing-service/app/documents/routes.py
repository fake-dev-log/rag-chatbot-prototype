import logging
from http import HTTPStatus

from fastapi import APIRouter, Depends, HTTPException

from app.dependencies import verify_api_key
from .schemas import DocumentRequest, IndexingResponse, DeleteResponse
from .service import DocumentService

logger = logging.getLogger(__name__)
router = APIRouter(prefix='/documents', dependencies=[Depends(verify_api_key)])
service = DocumentService()


@router.post(
    "",
    response_model=IndexingResponse,
    summary="Add a new document to the vector store"
)
async def add_document(request: DocumentRequest):
    try:
        response = service.add_document(request.file_path)
        await service.trigger_rag_retriever_reload()
        return response
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
        response = service.update_document(request.file_path)
        await service.trigger_rag_retriever_reload()
        return response
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
        response = service.delete_document(file_name)
        if response.get("is_deleted"):
            await service.trigger_rag_retriever_reload()
        return response
    except Exception as e:
        logger.error(f"Error deleting document {file_name}: {e}")
        raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))