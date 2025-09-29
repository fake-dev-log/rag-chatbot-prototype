package prototype.coreapi.domain.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import prototype.coreapi.domain.document.dto.IndexingRequest;
import prototype.coreapi.domain.document.entity.Document;
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

    private final DocumentRepository documentRepository;
    private final WebClient indexingWebClient;

    public DocumentService(DocumentRepository documentRepository,
                           WebClientFactory webClientFactory) {
        this.documentRepository = documentRepository;
        this.indexingWebClient = webClientFactory.getWebClient(WebClientFactory.ServiceType.INDEXING);
    }

    @Value("${document.storage.path}")
    private String documentStoragePath;

    public Flux<DocumentResponse> findAll() {
        return documentRepository.findAll()
                .map(DocumentResponse::from);
    }

    public Mono<List<String>> findAllCategories() {
        return documentRepository.findDistinctCategories().collectList();
    }

    public Mono<DocumentResponse> upload(FilePart filePart, String category) {
        Path storageDirectory = Paths.get(documentStoragePath);
        String originalFilename = filePart.filename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path destinationFile = storageDirectory.resolve(storedFilename);

        return filePart.transferTo(destinationFile)
                .then(Mono.defer(() -> {
                    return Mono.fromCallable(() -> {
                        File file = destinationFile.toFile();
                        String fileType = getFileExtension(originalFilename);
                        return Document.builder()
                                .name(originalFilename)
                                .path(destinationFile.toString())
                                .type(fileType)
                                .size(file.length())
                                .category(category) // Add category here
                                .build();
                    }).subscribeOn(Schedulers.boundedElastic())
                            .flatMap(documentRepository::save);
                }))
                .flatMap(savedDocument ->
                    triggerIndexing(savedDocument.getPath(), savedDocument.getName(), savedDocument.getCategory()) // Pass category here
                            .thenReturn(savedDocument)
                            .onErrorResume(e -> {
                                log.error("Indexing failed for {}. Rolling back document save.", savedDocument.getPath(), e);
                                Mono<Void> rollbackSequence = Mono.fromRunnable(() -> {
                                    File file = new File(savedDocument.getPath());
                                    if (file.exists() && !file.delete()) {
                                        log.warn("Failed to delete physical file on rollback: {}", savedDocument.getPath());
                                    }
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(documentRepository.delete(savedDocument));

                                return rollbackSequence.then(Mono.error(e));
                            })
                )
                .map(DocumentResponse::from);
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
                            .then(triggerDeindexing(document.getName()))
                            .thenReturn(document)
                )
                .flatMap(document ->
                    documentRepository.deleteById(document.getId())
                );
    }

    private Mono<Void> triggerIndexing(String filePath, String originalFilename, String category) {
        return indexingWebClient.post()
                .uri("/documents")
                .bodyValue(new IndexingRequest(
                        filePath,
                        originalFilename,
                        category // Add category here
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .doOnError(e -> log.error("Failed to trigger indexing service for file: {}", filePath, e));
    }

    private Mono<Void> triggerDeindexing(String fileName) {
        return indexingWebClient.delete()
                        .uri("/documents/{fileName}", fileName)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .doOnError(e -> log.error("Failed to trigger de-indexing service for file: {}", fileName, e));
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
