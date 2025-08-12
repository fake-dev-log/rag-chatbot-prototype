package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

import static prototype.coreapi.global.enums.RedisKeyPrefix.REFRESH_TOKEN;

@Component
@RequiredArgsConstructor
public class RefreshTokenStoreProvider {

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN.key(userId), token, ttl);
    }

    public Optional<String> get(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_TOKEN.key(userId)));
    }

    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN.key(userId));
    }
}
