package prototype.coreapi.domain.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import prototype.coreapi.domain.prompt.dto.ApplyPromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import prototype.coreapi.domain.prompt.entity.PromptTemplate;
import prototype.coreapi.domain.prompt.mapper.PromptMapper;
import prototype.coreapi.domain.prompt.repository.PromptTemplateRepository;
import prototype.coreapi.global.config.WebClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.http.MediaType;

import java.time.Duration;

@Slf4j
@Service
public class PromptService {

    private final PromptTemplateRepository promptRepository;
    private final PromptMapper promptMapper;
    private final WebClient ragWebClient;

    public PromptService(PromptTemplateRepository promptRepository,
                         PromptMapper promptMapper,
                         WebClientFactory webClientFactory) {
        this.promptRepository = promptRepository;
        this.promptMapper = promptMapper;
        this.ragWebClient = webClientFactory.getWebClient(WebClientFactory.ServiceType.RAG);
    }

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
                .flatMap(savedPrompt -> triggerPromptReload()
                        .thenReturn(savedPrompt))
                .map(promptMapper::toResponse);
    }

    public Mono<PromptResponse> update(Long id, PromptRequest request) {
        return promptRepository.findById(id)
                .flatMap(promptTemplate -> {
                    promptTemplate.update(request.getName(), request.getTemplateContent());
                    return promptRepository.save(promptTemplate);
                })
                .flatMap(savedPrompt -> triggerPromptReload()
                        .thenReturn(savedPrompt))
                .map(promptMapper::toResponse);
    }

    public Mono<String> apply(Long id) {
        return findById(id)
                .flatMap(prompt -> {

                    ApplyPromptRequest requestBody = ApplyPromptRequest.builder()
                            .name(prompt.getName())
                            .templateContent(prompt.getTemplateContent())
                            .build();

                    return ragWebClient.post()
                            .uri("/prompts/apply")
                            .accept(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnSuccess(response -> log.info("Successfully applied prompt: {}", response))
                            .doOnError(WebClientResponseException.class, err -> {
                                log.error("Failed to apply prompt. Status: {}, Body: {}",
                                        err.getStatusCode(), err.getResponseBodyAsString());
                            });
                });
    }

    public Mono<Void> deleteById(Long id) {
        return promptRepository.deleteById(id)
                .then(triggerPromptReload());
    }

    private Mono<Void> triggerPromptReload() {
        return ragWebClient.post()
                .uri("/prompts/reload")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .doOnError(e -> log.error("Failed to trigger prompt reload in rag-service", e));
    }
}
