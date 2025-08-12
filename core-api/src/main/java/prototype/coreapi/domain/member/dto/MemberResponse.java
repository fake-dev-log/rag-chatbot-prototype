package prototype.coreapi.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import prototype.coreapi.global.enums.Role;
import prototype.coreapi.global.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "회원 정보 응답")
public class MemberResponse {

    @Schema(description = "PK", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "회원 역할 타입", example = "USER")
    private Role role;

    @Schema(description = "회원 상태 타입", example = "ACTIVE")
    private Status status;

    @Schema(description = "마지막 로그인 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime lastLoginAt;

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-01T10:00:00")
    private LocalDateTime updatedAt;
}
