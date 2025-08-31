package prototype.coreapi.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import prototype.coreapi.global.enums.Role;
import prototype.coreapi.global.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "Member information response")
public class MemberResponse {

    @Schema(description = "PK", example = "1")
    private Long id;

    @Schema(description = "Email", example = "test@example.com")
    private String email;

    @Schema(description = "Member role type", example = "USER")
    private Role role;

    @Schema(description = "Member status type", example = "ACTIVE")
    private Status status;

    @Schema(description = "Last sign-in time", example = "2024-01-01T10:00:00")
    private LocalDateTime lastSignInAt;

    @Schema(description = "Created at", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Updated at", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
}