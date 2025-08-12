from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)


class SourceDocument(BaseModel):
    file_name: str
    title: Optional[str] = None
    page_number: int
    snippet: str