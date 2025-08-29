package prototype.coreapi.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import prototype.coreapi.global.util.TokenHashUtil;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static prototype.coreapi.global.enums.RedisKeyPrefix.ACCESS_TOKEN_BLACKLIST;

@Component
@RequiredArgsConstructor
public class TokenBlacklistStoreProvider {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<Boolean> blacklistHashed(String hashedToken, Duration ttl) {
        String key = ACCESS_TOKEN_BLACKLIST.key(hashedToken);
        return redisTemplate.opsForValue().set(key, "blacklisted", ttl);
    }

    public Mono<Boolean> isBlacklisted(String token) {
        String key = ACCESS_TOKEN_BLACKLIST.key(TokenHashUtil.sha256(token));
        return redisTemplate.hasKey(key);
    }
}

