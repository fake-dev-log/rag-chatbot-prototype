package prototype.coreapi.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "챗봇 질의에 대한 응답")
public class ChatbotResponse {

    @Schema(description = "답변", example = "나도 몰라.")
    private String answer;

    @Schema(description = "출처 목록", example = "LIST<SourceDocument>")
    List<SourceDocumentProjection> sources;
}
