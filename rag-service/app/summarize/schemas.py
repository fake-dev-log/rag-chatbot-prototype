from pydantic import BaseModel, Field
from pydantic.alias_generators import to_camel


class SummarizationRequest(BaseModel):
    previous_summary: str | None = Field(
        None,
        alias="previousSummary",
        description="The summary of the conversation so far.",
        example="The user asked about RAG chatbots...",
    )
    new_question: str = Field(
        ...,
        alias="newQuestion",
        description="The new question from the user.",
        example="How is conversation memory implemented?",
    )
    new_answer: str = Field(
        ...,
        alias="newAnswer",
        description="The new answer from the AI.",
        example="Progressive summarization is a good approach...",
    )


class SummarizationResponse(BaseModel):
    summary: str = Field(
        ...,
        description="The newly generated summary.",
        example="The user asked how to implement conversation memory, and the AI recommended progressive summarization.",
    )

    class Config:
        alias_generator = to_camel
        populate_by_name = True
