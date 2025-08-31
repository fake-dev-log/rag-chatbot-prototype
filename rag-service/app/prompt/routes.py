from http import HTTPStatus
from fastapi import APIRouter, Depends, Response
from app.dependencies import verify_api_key
from app.modules.prompt_loader import prompt_loader

router = APIRouter(prefix='/prompts', dependencies=[Depends(verify_api_key)])

@router.post("/reload", summary="Reload the prompt template cache")
async def reload_prompts():
    """
    Triggers a reload of the prompt template cache from the core-api.
    """
    try:
        prompt_loader.clear_cache()
        return Response(status_code=HTTPStatus.NO_CONTENT)
    except Exception as e:
        return Response(status_code=HTTPStatus.INTERNAL_SERVER_ERROR, content=f"Failed to reload prompts: {e}")
