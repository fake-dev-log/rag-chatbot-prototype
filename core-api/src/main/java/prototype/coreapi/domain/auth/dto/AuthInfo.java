package prototype.coreapi.domain.auth.dto;

import prototype.coreapi.global.enums.Role;

import java.time.Duration;
import java.time.LocalDateTime;

public record AuthInfo(
        String accessToken,
        String refreshToken,
        Duration refreshTokenTtl,
        Long id,
        String email,
        Role role,
        LocalDateTime lastLoginAt
) {
}
