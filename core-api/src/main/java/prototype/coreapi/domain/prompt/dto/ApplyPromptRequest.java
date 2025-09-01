package prototype.coreapi.domain.prompt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Builder
public class ApplyPromptRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String templateContent;
}
