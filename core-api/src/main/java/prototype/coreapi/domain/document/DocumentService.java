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
import java.util.UUID;

import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import java.time.Duration;

/**
 * Service responsible for managing the lifecycle of documents.
 * This includes handling file uploads, database persistence, and coordinating
 * with the external indexing service.
 */
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

    /**
     * Retrieves all document records from the database.
     *
     * @return A Flux of DocumentResponse objects.
     */
    public Flux<DocumentResponse> findAll() {
        return documentRepository.findAll()
                .map(DocumentResponse::from);
    }

    /**
     * Handles the upload of a new document.
     * This method performs a multistep process:
     * 1. Saves the file to the local disk.
     * 2. Creates a corresponding record in the database.
     * 3. Triggers the indexing service to process the new file.
     * If step 3 fails, it attempts to roll back steps 1 and 2.
     *
     * @param filePart A Mono emitting the uploaded file part.
     * @return A Mono emitting the response DTO for the created document.
     */
    public Mono<DocumentResponse> upload(FilePart filePart) {
        Path storageDirectory = Paths.get(documentStoragePath);
        String originalFilename = filePart.filename();
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        Path destinationFile = storageDirectory.resolve(storedFilename);

        // Step 1: Save the file to the disk.
        return filePart.transferTo(destinationFile)
                .then(Mono.defer(() -> {
                    // Step 2: Create and save a Document entity to the database.
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
                    // Step 3: Trigger the external indexing service.
                    triggerIndexing(savedDocument.getPath(), savedDocument.getName())
                            .thenReturn(savedDocument)
                            .onErrorResume(e -> {
                                log.error("Indexing failed for {}. Rolling back document save.", savedDocument.getPath(), e);
                                // Define the rollback sequence.
                                Mono<Void> rollbackSequence = Mono.fromRunnable(() -> {
                                    File file = new File(savedDocument.getPath());
                                    if (file.exists() && !file.delete()) {
                                        log.warn("Failed to delete physical file on rollback: {}", savedDocument.getPath());
                                    }
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(documentRepository.delete(savedDocument));

                                // Execute the rollback and then propagate the original error.
                                return rollbackSequence.then(Mono.error(e));
                            })
                )
                .map(DocumentResponse::from);
    }

    /**
     * Deletes a document by its ID.
     * This involves deleting the physical file, triggering de-indexing, and deleting the database record.
     *
     * @param documentId The ID of the document to delete.
     * @return A Mono that completes when the deletion is finished.
     */
    public Mono<Void> deleteById(Long documentId) {
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found")))
                .flatMap(document ->
                    // Step 1: Delete the physical file from disk.
                    Mono.fromRunnable(() -> {
                        File file = new File(document.getPath());
                        if (file.exists() && !file.delete()) {
                            log.warn("Failed to delete physical file: {}", document.getPath());
                        }
                    }).subscribeOn(Schedulers.boundedElastic())
                            .then(triggerDeindexing(document.getName())) // Step 2: Trigger the de-indexing service.
                            .thenReturn(document) // Pass the document object to the next step.
                )
                .flatMap(document ->
                    // 3. Delete DB record
                    documentRepository.deleteById(document.getId())
                );
    }

    /**
     * Triggers the external indexing service to process a file.
     * @param filePath The path to the file to be indexed.
     * @param originalFilename The original filename for metadata.
     * @return A Mono<Void> indicating completion of the indexing request.
     */
    private Mono<Void> triggerIndexing(String filePath, String originalFilename) {
        return indexingWebClient.post()
                .uri("/documents")
                .bodyValue(new IndexingRequest(
                        filePath,
                        originalFilename
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .doOnError(e -> log.error("Failed to trigger indexing service for file: {}", filePath, e));
    }

    /**
     * Triggers the external indexing service to remove a file from the index.
     * @param fileName The name of the file to be de-indexed.
     * @return A Mono<Void> indicating completion of the de-indexing request.
     */
    private Mono<Void> triggerDeindexing(String fileName) {
        return indexingWebClient.delete()
                        .uri("/documents/{fileName}", fileName)
                        .retrieve()
                        .bodyToMono(Void.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                        .doOnError(e -> log.error("Failed to trigger de-indexing service for file: {}", fileName, e));
    }

    /**
     * Extracts the file extension from a given filename.
     * @param filename The full name of the file.
     * @return The file extension (e.g., "pdf", "txt"), or an empty string if no extension is found.
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}