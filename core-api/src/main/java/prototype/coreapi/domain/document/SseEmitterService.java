package prototype.coreapi.domain.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class SseEmitterService {

    private final Sinks.Many<DocumentResponse> sink;

    public SseEmitterService() {
        // Use a replay sink with a limit of 1. This creates a hot stream that replays the last
        // event to new subscribers, which is useful for them to get the most recent status immediately.
        // It also doesn't terminate when subscribers disconnect.
        this.sink = Sinks.many().replay().limit(1);
        log.info("SseEmitterService initialized with a replay(1) sink.");
    }

    public void send(DocumentResponse document) {
        Sinks.EmitResult result = sink.tryEmitNext(document);

        if (result.isFailure()) {
            // Log only failures to avoid cluttering the logs
            log.warn("SSE event send failed for document id: {}. Reason: {}", document.id(), result);
        }
    }

    public Flux<ServerSentEvent<DocumentResponse>> connect() {
        log.debug("New SSE client connected. Current subscriber count: {}", sink.currentSubscriberCount());
        return sink.asFlux()
                .doOnNext(document -> log.debug("Pushing update for document id: {} to a client.", document.id()))
                .doOnCancel(() -> log.debug("SSE client disconnected. Current subscriber count: {}", sink.currentSubscriberCount()))
                .map(document -> ServerSentEvent.builder(document).build());
    }
}
