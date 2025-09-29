from pydantic import BaseModel, Field


class DocumentRequest(BaseModel):
    file_path: str = Field(..., description="The absolute path to the document file within the shared volume.")
    document_name: str = Field(..., description="The name of the document.")
    category: str | None = Field(None, description="The category of the document.")


class IndexingResponse(BaseModel):
    file_name: str
    detail: str


class DeleteResponse(BaseModel):
    file_name: str
    is_deleted: bool
    detail: str
