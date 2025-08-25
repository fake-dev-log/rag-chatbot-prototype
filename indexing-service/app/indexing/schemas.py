from pydantic import BaseModel, Field


class DocumentRequest(BaseModel):
    file_path: str = Field(..., description="The absolute path to the document file within the shared volume.")


class IndexingResponse(BaseModel):
    file_name: str
    detail: str


class DeleteResponse(BaseModel):
    file_name: str
    is_deleted: bool
    detail: str
