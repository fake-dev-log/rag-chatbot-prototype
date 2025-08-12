package prototype.coreapi.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import prototype.coreapi.domain.message.dto.MessageResponse;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "대화 응답")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatResponse {

    @Schema(description = "대화 PK", example = "1")
    private Long id;

    @Schema(description = "회원 이메일", example = "test@example.com")
    private String memberEmail;

    @Schema(description = "대화 제목", example = "2025년 06월 02일의 대화")
    private String title;

    @Schema(description = "최근 메시지 미리보기", example = "이게 뭐야?")
    private String lastMessagePreview;

    @Schema(description = "아카이브 여부", example = "false")
    private boolean isArchived;

    @Schema(description = "메세지 리스트", example = "LIST<MessageResponse>")
    private List<MessageResponse> messages;

    @Schema(description = "생성일시", example = "2025-06-03 00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-06-03 00:00:00")
    private LocalDateTime updatedAt;
}
