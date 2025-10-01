package prototype.coreapi.domain.document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import prototype.coreapi.domain.document.dto.IndexingJobPayload;
import prototype.coreapi.domain.document.entity.Document;
import prototype.coreapi.domain.document.enums.IndexingStatus;
import prototype.coreapi.domain.document.repository.DocumentRepository;
import prototype.coreapi.global.config.WebClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import java.time.Duration;

@Slf4j
@Service
public class DocumentService {

    private static final String INDEXING_QUEUE_KEY = "document-indexing-queue";

    private final DocumentRepository documentRepository;
    private final WebClient indexingWebClient;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    public DocumentService(DocumentRepository documentRepository,
                           WebClientFactory webClientFactory,
                           ReactiveStringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           SseEmitterService sseEmitterService) {
        this.documentRepository = documentRepository;
        this.indexingWebClient = webClientFactory.getWebClient(WebClientFactory.ServiceType.INDEXING);
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sseEmitterService = sseEmitterService;
    }

    public Flux<DocumentResponse> findAll() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .map(DocumentResponse::from);
    }

    public Mono<List<String>> findAllCategories() {
        return documentRepository.findDistinctCategories().collectList();
    }

    public Mono<DocumentResponse> upload(FilePart filePart, String category) {
        Path storageDirectory = Paths.get(documentStoragePath);
        String originalFilename = filePart.filename();
        String fileType = getFileExtension(originalFilename);
        String storedName = UUID.randomUUID() + "." + fileType;
        Path destinationFile = storageDirectory.resolve(storedName);

        // 1. Save the physical file
        return filePart.transferTo(destinationFile)
                .then(Mono.defer(() -> {
                    // 2. Create and save the document entity with PENDING status
                    File file = destinationFile.toFile();
                    Document document = Document.builder()
                            .name(originalFilename)
                            .storedName(storedName)
                            .path(destinationFile.toString())
                            .type(fileType)
                            .size(file.length())
                            .category(category)
                            .status(IndexingStatus.PENDING)
                            .build();
                    return documentRepository.save(document);
                }))
                .flatMap(savedDocument ->
                        // 3. Publish a job to the Redis queue
                        publishIndexingJob(savedDocument)
                                .thenReturn(savedDocument)
                )
                .map(DocumentResponse::from)
                .doOnSuccess(response -> {
                    log.info("Successfully uploaded document {} and published indexing job.", response.name());
                    sseEmitterService.send(response); // Also send initial PENDING status via SSE
                })
                .doOnError(e -> log.error("Failed during document upload or job publishing for file: {}", originalFilename, e));
    }

    public Mono<Document> updateStatus(Long documentId, IndexingStatus status) {
        return documentRepository.findById(documentId)
                .flatMap(document -> {
                    document.updateStatus(status);
                    return documentRepository.save(document);
                })
                .doOnSuccess(updatedDocument -> {
                    log.info("Updated status to {} for document id: {}", status, documentId);
                    sseEmitterService.send(DocumentResponse.from(updatedDocument));
                });
    }

    private Mono<Long> publishIndexingJob(Document document) {
        return Mono.fromCallable(() -> {
            try {
                IndexingJobPayload payload = IndexingJobPayload.builder()
                        .documentId(document.getId())
                        .storedName(document.getStoredName())
                        .category(document.getCategory())
                        .build();
                return objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing indexing job payload", e);
            }
        })
        .flatMap(payload -> redisTemplate.opsForList().leftPush(INDEXING_QUEUE_KEY, payload))
        .doOnSuccess(jobId -> log.info("Published indexing job for document id: {}", document.getId()));
    }

    public Mono<Void> deleteById(Long documentId) {
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found")))
                .flatMap(document ->
                        Mono.fromRunnable(() -> {
                            File file = new File(document.getPath());
                            if (file.exists() && !file.delete()) {
                                log.warn("Failed to delete physical file: {}", document.getPath());
                            }
                        }).subscribeOn(Schedulers.boundedElastic())
                                .then(triggerDeindexing(document.getStoredName())) // Use storedName for de-indexing
                                .thenReturn(document)
                )
                .flatMap(document ->
                        documentRepository.deleteById(document.getId())
                );
    }

    private Mono<Void> triggerDeindexing(String storedName) {
        return indexingWebClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/documents")
                        .queryParam("file_name", storedName)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .doOnError(e -> log.error("Failed to trigger de-indexing service for file: {}", storedName, e));
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
