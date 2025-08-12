package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import prototype.coreapi.global.util.TokenHashUtil;

import java.time.Duration;

import static prototype.coreapi.global.enums.RedisKeyPrefix.ACCESS_TOKEN_HASH;

@Component
@RequiredArgsConstructor
public class AccessTokenStoreProvider {

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String token, Duration ttl) {
        String hashed = TokenHashUtil.sha256(token);
        String key = ACCESS_TOKEN_HASH.key(userId);
        redisTemplate.opsForValue().set(key, hashed, ttl);
    }
}