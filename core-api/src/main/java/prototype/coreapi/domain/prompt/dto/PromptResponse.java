package prototype.coreapi.domain.prompt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Response containing prompt template details")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptResponse {
    @Schema(description = "Prompt Template ID", example = "1")
    private Long id;

    @Schema(description = "The unique name of the prompt template", example = "Medical_Device_Cybersecurity_Template_V2")
    private String name;

    @Schema(description = "The content of the prompt template")
    private String templateContent;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
