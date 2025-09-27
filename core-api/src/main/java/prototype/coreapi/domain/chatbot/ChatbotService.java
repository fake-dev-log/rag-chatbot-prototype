package prototype.coreapi.domain.chatbot;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.chatbot.dto.*;
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

    public Flux<ChatChunk> inference(String question, String chatHistory) {
        return performPreflightCheck()
                .thenMany(performInference(question, chatHistory));
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

    private Flux<ChatChunk> performInference(String question, String chatHistory) {
        ChatbotRequest reqDto = new ChatbotRequest(question, chatHistory);

        return ragWebClient.post()
                .uri("/chats")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToFlux(ChatChunk.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("LLM Error", throwable));
    }

    public Mono<SummarizationResponse> summarize(String previousSummary, String newQuestion, String newAnswer) {
        SummarizationRequest reqDto = new SummarizationRequest(previousSummary, newQuestion, newAnswer);

        return ragWebClient.post()
                .uri("/summarize")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToMono(SummarizationResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("Summarization Error", throwable));
    }

    public Mono<TitleGenerationResponse> generateTitle(String question, String answer) {
        TitleGenerationRequest reqDto = new TitleGenerationRequest(question, answer);

        return ragWebClient.post()
                .uri("/generate-title")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToMono(TitleGenerationResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("Title Generation Error", throwable));
    }
}