from pydantic import BaseModel
from pydantic.alias_generators import to_camel


class TitleGenerationRequest(BaseModel):
    question: str
    answer: str


class TitleGenerationResponse(BaseModel):
    title: str

    class Config:
        alias_generator = to_camel
        populate_by_name = True
