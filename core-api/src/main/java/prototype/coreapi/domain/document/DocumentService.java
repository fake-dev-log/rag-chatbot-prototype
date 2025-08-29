package prototype.coreapi.domain.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import prototype.coreapi.domain.document.entity.Document;
import prototype.coreapi.domain.document.repository.DocumentRepository;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WebClient webClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    @Value("${document.storage.path}")
    private String documentStoragePath;

    @Value("${indexing-service.api.baseUrl}")
    private String indexingServiceBaseUrl;

    public Flux<DocumentResponse> findAll() {
        return documentRepository.findAll()
                .map(DocumentResponse::from);
    }

    public Mono<DocumentResponse> upload(FilePart filePart) {
        Path storageDirectory = Paths.get(documentStoragePath);
        String originalFilename = filePart.filename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path destinationFile = storageDirectory.resolve(storedFilename);

        // 1. Save file to disk
        return filePart.transferTo(destinationFile)
                // 2. 파일 저장이 완료되면 'then'을 통해 다음 작업을 수행합니다.
                .then(Mono.defer(() -> {
            // 2. Create and save document entity to DB
            return Mono.fromCallable(() -> {
                File file = destinationFile.toFile();
                String fileType = getFileExtension(originalFilename);
                return Document.builder()
                        .name(originalFilename)
                        .path(destinationFile.toString())
                        .type(fileType)
                        .size(file.length())
                        .build();
            }).subscribeOn(Schedulers.boundedElastic())
                    .flatMap(documentRepository::save);
        }))
                .flatMap(savedDocument ->
                    // 3. Trigger indexing service
                    triggerIndexing(savedDocument.getPath())
                            .thenReturn(savedDocument) // Return the document after triggering
                            .onErrorResume(e -> {
                                log.error("Indexing failed for {}. Rolling back document save.", savedDocument.getPath(), e);
                                // Delete physical file
                                return Mono.fromRunnable(() -> {
                                    File file = new File(savedDocument.getPath());
                                    if (file.exists()) {
                                        if (!file.delete()) {
                                            log.warn("Failed to delete physical file on rollback: {}", savedDocument.getPath());
                                        }
                                    }
                                }).subscribeOn(Schedulers.boundedElastic())
                                        .then(documentRepository.delete(savedDocument)) // Delete DB record
                                        .then(Mono.error(e)); // Then propagate the error
                            })
                )
                .map(DocumentResponse::from);
    }

    public Mono<Void> deleteById(Long documentId) {
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found")))
                .flatMap(document ->
                    // 1. Delete physical file
                    Mono.fromRunnable(() -> {
                        File file = new File(document.getPath());
                        if (file.exists()) {
                            if (!file.delete()) {
                                log.warn("Failed to delete physical file: {}", document.getPath());
                            }
                        }
                    }).subscribeOn(Schedulers.boundedElastic())
                            .then(triggerDeindexing(document.getName())) // 2. Trigger de-indexing service
                            .thenReturn(document) // Pass document to the next step
                )
                .flatMap(document ->
                    // 3. Delete DB record
                    documentRepository.deleteById(document.getId())
                );
    }

    private Mono<Void> triggerIndexing(String filePath) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();
        record IndexingRequest(String file_path) {}

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(webClient.post()
                        .uri(indexingServiceBaseUrl + "/documents")
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .bodyValue(new IndexingRequest(filePath))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .doOnError(e -> log.error("Failed to trigger indexing service for file: {}", filePath, e))
                );
    }

    private Mono<Void> triggerDeindexing(String fileName) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(webClient.delete()
                        .uri(indexingServiceBaseUrl + "/documents/{fileName}", fileName)
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .doOnError(e -> log.error("Failed to trigger de-indexing service for file: {}", fileName, e))
                );
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
