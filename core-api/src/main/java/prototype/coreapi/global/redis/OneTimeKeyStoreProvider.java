package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OneTimeKeyStoreProvider {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> save(String key, String secret) {
        return redisTemplate.opsForValue().set(key, secret, Duration.ofSeconds(30));
    }

    public Mono<Boolean> save(String key, String secret, Duration expiration) {
        return redisTemplate.opsForValue().set(key, secret, expiration);
    }
}
