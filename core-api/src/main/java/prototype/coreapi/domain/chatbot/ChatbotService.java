package prototype.coreapi.domain.chatbot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.chatbot.dto.ChatChunk;
import prototype.coreapi.domain.chatbot.dto.ChatbotRequest;
import prototype.coreapi.domain.chatbot.dto.ReadyResponse;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final WebClient webClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    public Flux<ChatChunk> inference(String question) {
        ChatbotRequest reqDto = new ChatbotRequest(question);

        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        // 1) 준비 상태 확인 → 2) 준비되었으면 POST 요청, 아니면 에러
        return oneTimeKeyStoreProvider.save(
                        key, secret
                )
                .then(webClient.get()
                        .uri("/ready")
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .retrieve()
                        .onStatus(status -> status.is5xxServerError() || status.value() == 503,
                                resp -> Mono.error(new IllegalStateException("Inference Service 가동 중")))
                        .bodyToMono(ReadyResponse.class))
                .flatMapMany(rr -> {
                    if (!rr.isReady()) {
                        return Mono.error(new IllegalStateException("Inference Service 준비 중"));
                    }
                    String newKey = UUID.randomUUID().toString();
                    String newSecret = UUID.randomUUID().toString();

                    return oneTimeKeyStoreProvider.save(
                            newKey, newSecret
                    ).thenMany(webClient.post()
                            .uri("/chats")
                            .header("X-API-KEY", newKey)
                            .header("X-API-SECRET", newSecret)
                            .bodyValue(reqDto)
                            .retrieve()
                            .bodyToFlux(ChatChunk.class)                        // 한 줄씩 ChatChunk 로 매핑
                            .onErrorMap(             // status 에러 처리
                                    clientResponse -> { throw new RuntimeException("LLM 에러"); },
                                    throwable -> throwable
                            ));
                });
    }
}
