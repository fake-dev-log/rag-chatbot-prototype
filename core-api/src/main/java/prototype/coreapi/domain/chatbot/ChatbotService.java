package prototype.coreapi.domain.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.chatbot.dto.ChatChunk;
import prototype.coreapi.domain.chatbot.dto.ChatbotRequest;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import reactor.util.retry.Retry;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final WebClient webClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    public Flux<ChatChunk> inference(String question) {
        return performPreflightCheck()
                .thenMany(performInference(question));
    }

    private Mono<Void> performPreflightCheck() {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(webClient.head()
                        .uri("/chats")
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .retrieve()
                        .onStatus(
                                status -> status.is5xxServerError() || status.value() == 503,
                                resp -> Mono.error(new IllegalStateException("Inference Service 준비 중"))
                        )
                        .toBodilessEntity()
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .then()
                );
    }

    private Flux<ChatChunk> performInference(String question) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();
        ChatbotRequest reqDto = new ChatbotRequest(question);

        return oneTimeKeyStoreProvider.save(key, secret)
                .thenMany(webClient.post()
                        .uri("/chats")
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .bodyValue(reqDto)
                        .retrieve()
                        .bodyToFlux(ChatChunk.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .onErrorMap(throwable -> new RuntimeException("LLM 에러", throwable))
                );
    }
}
