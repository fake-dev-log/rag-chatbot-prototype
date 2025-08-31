package prototype.coreapi.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import prototype.coreapi.domain.message.dto.MessageResponse;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "Chat response")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ChatResponse {

    @Schema(description = "Chat PK", example = "1")
    private Long id;

    @Schema(description = "Member email", example = "test@example.com")
    private String memberEmail;

    @Schema(description = "Chat title", example = "2025-06-02 Chat")
    private String title;

    @Schema(description = "Last message preview", example = "What is this?")
    private String lastMessagePreview;

    @Schema(description = "Archived status", example = "false")
    private boolean isArchived;

    @Schema(description = "Message list", example = "LIST<MessageResponse>")
    private List<MessageResponse> messages;

    @Schema(description = "Created at", example = "2025-06-03 00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at", example = "2025-06-03 00:00:00")
    private LocalDateTime updatedAt;
}