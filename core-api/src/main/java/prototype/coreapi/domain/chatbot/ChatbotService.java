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

    private Mono<Void> performPreflightCheck() {
        return ragWebClient.head()
                .uri("/chats") // A simple endpoint to check if the service is up
                .retrieve()
                .onStatus(
                        status -> status.is5xxServerError() || status.value() == 503,
                        resp -> Mono.error(new IllegalStateException("RAG Service is not ready or unavailable"))
                )
                .toBodilessEntity()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .then();
    }

    public Flux<ChatChunk> inference(String question, String chatHistory, String category) {
        return performPreflightCheck()
                .thenMany(performInference(question, chatHistory, category));
    }

    public Mono<SummarizationResponse> summarize(String previousSummary, String newQuestion, String newAnswer) {
        return performPreflightCheck()
                .then(performSummarize(previousSummary, newQuestion, newAnswer));
    }

    public Mono<TitleGenerationResponse> generateTitle(String question, String answer) {
        return performPreflightCheck()
                .then(performGenerateTitle(question, answer));
    }

    private Flux<ChatChunk> performInference(String question, String chatHistory, String category) {
        ChatbotRequest reqDto = new ChatbotRequest(question, chatHistory, category);

        return ragWebClient.post()
                .uri("/chats")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToFlux(ChatChunk.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("LLM Error", throwable));
    }

    private Mono<SummarizationResponse> performSummarize(String previousSummary, String newQuestion, String newAnswer) {
        SummarizationRequest reqDto = new SummarizationRequest(previousSummary, newQuestion, newAnswer);

        return ragWebClient.post()
                .uri("/summarize")
                .bodyValue(reqDto)
                .retrieve()
                .bodyToMono(SummarizationResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorMap(throwable -> new RuntimeException("Summarization Error", throwable));
    }

    private Mono<TitleGenerationResponse> performGenerateTitle(String question, String answer) {
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
