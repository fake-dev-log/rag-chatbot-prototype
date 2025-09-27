from fastapi import APIRouter, Depends
from app.dependencies import verify_api_key
from app.title_generator.schemas import TitleGenerationRequest, TitleGenerationResponse
from app.title_generator.service import title_generation_service

router = APIRouter()


@router.post(
    "/generate-title",
    response_model=TitleGenerationResponse,
    response_model_by_alias=True,
    dependencies=[Depends(verify_api_key)]
)
def generate_title_endpoint(request: TitleGenerationRequest):
    """
    Generates a title for a conversation based on the first turn.
    """
    title = title_generation_service.generate_title(request)
    return TitleGenerationResponse(title=title)
