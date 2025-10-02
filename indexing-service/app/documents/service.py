import logging
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

    def add_document(self, file_path: str, doc_id: int, original_filename: str, category: str | None = None):
        """Adds a new document to the vector store."""
        try:
            self.data_processor.add_document(file_path, doc_id, original_filename, category)
            return {"original_filename": original_filename, "detail": "Document added successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error adding document {original_filename}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def update_document(self, file_path: str, doc_id: int, original_filename: str, category: str | None = None):
        """Updates an existing document in the vector store."""
        try:
            self.data_processor.update_document(file_path, doc_id, original_filename, category)
            return {"original_filename": original_filename, "detail": "Document updated successfully."}
        except FileNotFoundError:
            raise HTTPException(status_code=HTTPStatus.NOT_FOUND, detail=f"File not found at path: {file_path}")
        except Exception as e:
            logger.error(f"Error updating document {original_filename}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))

    def delete_document(self, doc_id: int):
        """Deletes a document and its corresponding vectors from the store."""
        logger.info(f"Deleting document with doc_id: {doc_id}")
        try:
            deleted = self.data_processor.delete_document(doc_id)
            if deleted:
                return {"doc_id": doc_id, "is_deleted": True, "detail": "Document deleted successfully."}
            else:
                return {"doc_id": doc_id, "is_deleted": False, "detail": "Document not found or already deleted."}
        except Exception as e:
            logger.error(f"Error deleting document with doc_id {doc_id}: {e}")
            raise HTTPException(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, detail=str(e))