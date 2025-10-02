from pydantic import BaseModel, Field
from pydantic.alias_generators import to_camel


class DocumentRequest(BaseModel):
    file_path: str = Field(..., description="The absolute path to the document file within the shared volume.")
    doc_id: int = Field(..., description="The unique ID of the document from the core-api database.")
    original_filename: str = Field(..., description="The original, human-readable name of the document.")
    category: str | None = Field(None, description="The category of the document.")

    class Config:
        alias_generator = to_camel
        populate_by_name = True


class IndexingResponse(BaseModel):
    original_filename: str = Field(..., alias="originalFilename")
    detail: str

    class Config:
        alias_generator = to_camel
        populate_by_name = True


class DeleteResponse(BaseModel):
    doc_id: int = Field(..., alias="docId")
    is_deleted: bool = Field(..., alias="isDeleted")
    detail: str

    class Config:
        alias_generator = to_camel
        populate_by_name = True