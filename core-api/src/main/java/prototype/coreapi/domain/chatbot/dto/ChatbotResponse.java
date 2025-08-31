package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "Response to chatbot query")
public class ChatbotResponse {

    @Schema(description = "Answer", example = "I don't know either.")
    private String answer;

    @Schema(description = "List of sources", example = "LIST<SourceDocument>")
    List<SourceDocumentProjection> sources;
}