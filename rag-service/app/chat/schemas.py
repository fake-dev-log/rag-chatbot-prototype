from pydantic import BaseModel, Field
from pydantic.alias_generators import to_camel


class ChatRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=1000)
    chat_history: str | None = Field(
        None,
        alias="chatHistory",
        description="The summary of the conversation so far, to be used as context.",
        example="The user asked about RAG chatbots..."
    )


class SourceDocument(BaseModel):
    file_name: str
    title: str | None = None
    page_number: int
    snippet: str

    class Config:
        alias_generator = to_camel
        populate_by_name = True
