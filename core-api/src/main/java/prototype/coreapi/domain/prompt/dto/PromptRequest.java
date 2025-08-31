package prototype.coreapi.domain.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Request to create or update a prompt template")
public class PromptRequest {
    @NotBlank
    @Schema(description = "The unique name of the prompt template", example = "Medical_Device_Cybersecurity_Template_V2")
    private String name;

    @NotBlank
    @Schema(description = "The content of the prompt template")
    private String templateContent;
}