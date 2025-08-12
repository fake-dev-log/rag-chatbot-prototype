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
@Schema(description = "메세지 응답")
public class MessageResponse {

    @Schema(description = "메시지 PK", example = "qwef1qewr12df4")
    private String id;

    @Schema(description = "챗 PK", example = "1")
    private Long chatId;

    @Schema(description = "보낸이", example = "USER or BOT")
    private Sender sender;

    @Schema(description = "내용", example = "이게 뭐야?")
    private Object content;

    @Schema(description = "유형", example = "text, image or etc.")
    private String contentType;

    @Schema(description = "메시지 순서", example = "1")
    private Long sequence;

    @Schema(description = "출처 목록", example = "LIST<SourceDocument>")
    List<SourceDocument> sources;

    @Schema(description = "작성일시", example = "2025-01-01 00:00:00")
    private Instant createdAt;
}