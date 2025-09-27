package prototype.coreapi.domain.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import prototype.coreapi.domain.prompt.mapper.PromptMapper;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/prompts")
@RequiredArgsConstructor
public class PromptInternalController {

    private final PromptService promptService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final PromptMapper promptMapper;

    @GetMapping("/name/{name}")
    public Mono<PromptResponse> getPromptByName(
            @RequestHeader("X-API-KEY") String key,
            @RequestHeader("X-API-SECRET") String secret,
            @PathVariable String name
    ) {
        // Verify the one-time API key against Redis
        return verifyApiKey(key, secret)
                .then(promptService.findByName(name));
    }

    private Mono<Void> verifyApiKey(String key, String secret) {
        return redisTemplate.opsForValue()
                .getAndDelete(key)
                .filter(storedSecret -> storedSecret.equals(secret))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.AUTH_FAILED)))
                .then();
    }
}
