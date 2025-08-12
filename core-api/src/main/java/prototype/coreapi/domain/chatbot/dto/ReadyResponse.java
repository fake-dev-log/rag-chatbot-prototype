package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "준비 상태 응답")
public class ReadyResponse {

    @Schema(description = "준비 상태", example = "true")
    private boolean ready;
}
