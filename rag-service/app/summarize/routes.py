from fastapi import APIRouter
from app.summarize.schemas import SummarizationRequest, SummarizationResponse
from app.summarize.service import summarization_service

router = APIRouter()


@router.post("/summarize", response_model=SummarizationResponse, response_model_by_alias=True)
def summarize_conversation(request: SummarizationRequest):
    """
    Summarizes a conversation.
    """
    summary = summarization_service.summarize(request)
    return SummarizationResponse(summary=summary)
