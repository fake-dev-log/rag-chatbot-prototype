from fastapi import APIRouter, Depends

from app.dependencies import verify_api_key
from app.summarize.schemas import SummarizationRequest, SummarizationResponse
from app.summarize.service import summarization_service

router = APIRouter()


@router.post("/summarize", response_model=SummarizationResponse, response_model_by_alias=True, dependencies=[Depends(verify_api_key)])
def summarize_conversation(request: SummarizationRequest):
    """
    Summarizes a conversation.
    """
    summary = summarization_service.summarize(request)
    return SummarizationResponse(summary=summary)
