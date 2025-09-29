import logging
import os
from http import HTTPStatus

from fastapi import HTTPException

from app.modules.data.processor import DataProcessor

logger = logging.getLogger(__name__)


class DocumentService:
    """
    Handles the business logic for document indexing, including adding, updating,
    and deleting documents in the vector store.
    """

    def __init__(self):
        """Initializes the service, setting up the data processor."""
        self.data_processor = DataProcessor()

    def add_document(self, file_path: str, document_name: str, category: str | None = None):
        """Adds a new document to the vector store."""
        try:
            self.data_processor.add_document(file_path, document_name, category)
            return {"file_name": document_name, "detail": "Document added successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error adding document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def update_document(self, file_path: str, document_name: str, category: str | None = None):
        """Updates an existing document in the vector store."""
        try:
            self.data_processor.update_document(file_path, document_name, category)
            return {"file_name": os.path.basename(file_path), "detail": "Document updated successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error updating document {file_path}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def delete_document(self, file_name: str):
        """Deletes a document and its corresponding vectors from the store."""
        logger.info(f"Deleting document {file_name}")
        try:
            deleted = self.data_processor.delete_document(file_name)
            if deleted:
                return {"file_name": file_name, "is_deleted": True, "detail": "Document deleted successfully."}
            else:
                return {"file_name": file_name, "is_deleted": False, "detail": "Document not found or already deleted."}
        except Exception as e:
            logger.error(f"Error deleting document {file_name}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))
