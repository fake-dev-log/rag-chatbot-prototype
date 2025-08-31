package prototype.coreapi.domain.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import prototype.coreapi.domain.message.document.SourceDocument;
import prototype.coreapi.global.enums.Sender;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@Schema(description = "Message response")
public class MessageResponse {

    @Schema(description = "Message PK", example = "qwef1qewr12df4")
    private String id;

    @Schema(description = "Chat PK", example = "1")
    private Long chatId;

    @Schema(description = "Sender", example = "USER or BOT")
    private Sender sender;

    @Schema(description = "Content", example = "What is this?")
    private Object content;

    @Schema(description = "Type", example = "text, image or etc.")
    private String contentType;

    @Schema(description = "Message sequence", example = "1")
    private Long sequence;

    @Schema(description = "List of sources", example = "LIST<SourceDocument>")
    List<SourceDocument> sources;

    @Schema(description = "Created at", example = "2025-01-01 00:00:00")
    private Instant createdAt;
}
