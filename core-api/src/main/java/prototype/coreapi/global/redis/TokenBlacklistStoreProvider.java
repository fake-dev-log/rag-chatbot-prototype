package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import prototype.coreapi.global.util.TokenHashUtil;

import java.time.Duration;

import static prototype.coreapi.global.enums.RedisKeyPrefix.ACCESS_TOKEN_BLACKLIST;

@Component
@RequiredArgsConstructor
public class TokenBlacklistStoreProvider {

    private final StringRedisTemplate redisTemplate;

    public void blacklistHashed(String hashedToken, Duration ttl) {
        String key = ACCESS_TOKEN_BLACKLIST.key(hashedToken);
        redisTemplate.opsForValue().set(key, "blacklisted", ttl);
    }

    public boolean isBlacklisted(String token) {
        String key = ACCESS_TOKEN_BLACKLIST.key(TokenHashUtil.sha256(token));
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

