package prototype.coreapi.domain.chatbot;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.chatbot.dto.ChatChunk;
import prototype.coreapi.domain.chatbot.dto.ChatbotRequest;
import prototype.coreapi.global.config.WebClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import reactor.util.retry.Retry;
import java.time.Duration;

@Service
public class ChatbotService {

    private final WebClient ragWebClient;

    public ChatbotService(WebClientFactory webClientFactory) {
        this.ragWebClient = webClientFactory.getWebClient(WebClientFactory.ServiceType.RAG);
    }

    public Flux<ChatChunk> inference(String question) {
        return performPreflightCheck()
                .thenMany(performInference(question));
    }

    private Mono<Void> performPreflightCheck() {
        return ragWebClient.head()
                .uri("/chats")
                .retrieve()
                .onStatus(
                        status -> status.is5xxServerError() || status.value() == 503,
                        resp -> Mono.error(new IllegalStateException("Inference Service is preparing"))
                )
                .toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .then();
    }

    private Flux<ChatChunk> performInference(String question) {
        ChatbotRequest reqDto = new ChatbotRequest(question);

        return ragWebClient.post()
                .uri("/chats")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToFlux(ChatChunk.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("LLM Error", throwable));
    }
}