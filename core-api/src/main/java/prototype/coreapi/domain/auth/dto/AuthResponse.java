package prototype.coreapi.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import prototype.coreapi.global.enums.Role;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "로그인 응답")
public class AuthResponse {

    @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String accessToken;

    @Schema(description = "회원 PK", example = "1")
    private Long id;

    @Schema(description = "email", example = "test@example.com")
    private String email;

    @Schema(description = "유저 역할", example = "USER")
    private Role role;

    @Schema(description = "최근 로그인 시각", example = "2024-02-01T12:00:00")
    private LocalDateTime lastLoginAt;
}
