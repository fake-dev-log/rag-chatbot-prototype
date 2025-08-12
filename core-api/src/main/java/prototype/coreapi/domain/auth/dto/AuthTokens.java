package prototype.coreapi.domain.auth.dto;

import java.time.Duration;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        Duration refreshTokenTtl
) {
}
