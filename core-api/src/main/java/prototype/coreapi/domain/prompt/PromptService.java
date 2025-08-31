package prototype.coreapi.domain.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.prompt.dto.PromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import prototype.coreapi.domain.prompt.entity.PromptTemplate;
import prototype.coreapi.domain.prompt.mapper.PromptMapper;
import prototype.coreapi.domain.prompt.repository.PromptTemplateRepository;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptTemplateRepository promptRepository;
    private final PromptMapper promptMapper;
    @Qualifier("chatbotWebClient")
    private final WebClient chatbotWebClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    public Flux<PromptResponse> findAll() {
        return promptRepository.findAll()
                .map(promptMapper::toResponse);
    }

    public Mono<PromptResponse> findById(Long id) {
        return promptRepository.findById(id)
                .map(promptMapper::toResponse);
    }

    public Mono<PromptResponse> findByName(String name) {
        return promptRepository.findByName(name)
                .map(promptMapper::toResponse);
    }

    public Mono<PromptResponse> create(PromptRequest request) {
        PromptTemplate promptTemplate = promptMapper.toEntity(request);
        return promptRepository.save(promptTemplate)
                .doOnSuccess(p -> triggerPromptReload())
                .map(promptMapper::toResponse);
    }

    public Mono<PromptResponse> update(Long id, PromptRequest request) {
        return promptRepository.findById(id)
                .flatMap(promptTemplate -> {
                    promptTemplate.update(request.getName(), request.getTemplateContent());
                    return promptRepository.save(promptTemplate);
                })
                .doOnSuccess(p -> triggerPromptReload())
                .map(promptMapper::toResponse);
    }

    public Mono<Void> deleteById(Long id) {
        return promptRepository.deleteById(id)
                .doOnSuccess(v -> triggerPromptReload());
    }

    private void triggerPromptReload() {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        Mono<Void> reloadRequestMono = chatbotWebClient.post()
                .uri("/prompts/reload")
                .accept(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", key)
                .header("X-API-SECRET", secret)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .doOnError(e -> log.error("Failed to trigger prompt reload in rag-service", e));

        oneTimeKeyStoreProvider.save(key, secret)
                .then(reloadRequestMono)
                .subscribe(
                        v -> log.info("Successfully triggered prompt reload."),
                        err -> log.error("Error during prompt reload trigger: {}", err.getMessage())
                );
    }
}
