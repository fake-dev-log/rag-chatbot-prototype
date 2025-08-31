package prototype.coreapi.domain.prompt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import prototype.coreapi.domain.prompt.dto.ApplyPromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/prompts")
@Tag(name = "Prompt Management", description = "APIs for managing prompt templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class PromptController {

    private final PromptService promptService;
    @Qualifier("chatbotWebClient")
    private final WebClient chatbotWebClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    @GetMapping
    @Operation(summary = "Get all prompt templates")
    public Flux<PromptResponse> getAllPrompts() {
        return promptService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single prompt template by ID")
    public Mono<PromptResponse> getPromptById(@PathVariable Long id) {
        return promptService.findById(id);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get a single prompt template by name")
    public Mono<PromptResponse> getPromptByName(@PathVariable String name) {
        return promptService.findByName(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new prompt template")
    public Mono<PromptResponse> createPrompt(@Valid @RequestBody PromptRequest request) {
        return promptService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing prompt template")
    public Mono<PromptResponse> updatePrompt(@PathVariable Long id, @Valid @RequestBody PromptRequest request) {
        return promptService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a prompt template")
    public Mono<Void> deletePrompt(@PathVariable Long id) {
        return promptService.deleteById(id);
    }

    @PostMapping("/{id}/apply")
    public Mono<String> applyPrompt(@PathVariable Long id) {
        return promptService.findById(id)
                .flatMap(prompt -> {
                    String key = UUID.randomUUID().toString();
                    String secret = UUID.randomUUID().toString();

                    ApplyPromptRequest requestBody = new ApplyPromptRequest(prompt.getName(), prompt.getTemplateContent());

                    return oneTimeKeyStoreProvider.save(key, secret)
                            .then(Mono.defer(() ->
                                chatbotWebClient.post()
                                    .uri("/chats/admin/prompts/apply")
                                    .accept(MediaType.APPLICATION_JSON)
                                    .header("X-API-KEY", key)
                                    .header("X-API-SECRET", secret)
                                    .bodyValue(requestBody)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .doOnSuccess(response -> log.info("Successfully applied prompt: {}", response))
                                    .doOnError(WebClientResponseException.class, err -> {
                                        log.error("Failed to apply prompt. Status: {}, Body: {}",
                                                err.getStatusCode(), err.getResponseBodyAsString());
                                    })
                            ));
                });
    }
}