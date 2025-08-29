package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

import static prototype.coreapi.global.enums.RedisKeyPrefix.REFRESH_TOKEN;

@Component
@RequiredArgsConstructor
public class RefreshTokenStoreProvider {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> save(Long userId, String token, Duration ttl) {
        return redisTemplate.opsForValue().set(REFRESH_TOKEN.key(userId), token, ttl);
    }

    public Mono<Optional<String>> get(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN.key(userId)).map(Optional::ofNullable).defaultIfEmpty(Optional.empty());
    }

    public Mono<Void> delete(Long userId) {
        return redisTemplate.delete(REFRESH_TOKEN.key(userId)).then();
    }
}
