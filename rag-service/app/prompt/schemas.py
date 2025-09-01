from pydantic import BaseModel


class ApplyPromptRequest(BaseModel):
    name: str
    templateContent: str
