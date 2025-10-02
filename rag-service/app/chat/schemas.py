from pydantic import BaseModel, Field
from pydantic.alias_generators import to_camel


class ChatRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)
    chat_history: str | None = Field(
        None,
        alias="chatHistory",
        description="The summary of the conversation so far, to be used as context.",
        examples=["The user asked about RAG chatbots..."],
    )
    category: str | None = Field(None, description="The category to filter the search by.")


class SourceDocument(BaseModel):
    file_name: str = Field(..., alias="fileName")
    title: str | None = None
    page_number: int = Field(..., alias="pageNumber")
    snippet: str

    class Config:
        alias_generator = to_camel
        populate_by_name = True
