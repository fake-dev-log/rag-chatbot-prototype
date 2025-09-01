from http import HTTPStatus
from fastapi import APIRouter, Depends, Response, HTTPException # Added HTTPException
from app.dependencies import verify_api_key
from app.modules.prompt.prompt_loader import prompt_loader
from app.chat.routes import service as chat_service_singleton # Import the singleton service
from app.prompt.schemas import ApplyPromptRequest

router = APIRouter(prefix='/prompts', dependencies=[Depends(verify_api_key)])


@router.post("/apply", summary="Applies a new prompt template and reloads the RAG chain") # Changed path to /apply
async def apply_prompt(request: ApplyPromptRequest):
    """
    Applies a new prompt template by updating the ChatService and reloading the RAG chain.
    """
    try:
        chat_service_singleton.apply_prompt(request.name, request.templateContent)
        return {"message": f"Prompt '{request.name}' applied successfully."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


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
