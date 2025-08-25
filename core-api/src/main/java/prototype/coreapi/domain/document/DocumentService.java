package prototype.coreapi.domain.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import prototype.coreapi.domain.document.repository.DocumentRepository;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WebClient webClient;
    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    @Value("${document.storage.path}")
    private String documentStoragePath;

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
                .then(Mono.defer(() -> {
                    // 2. Create and save document entity to DB
                    File file = destinationFile.toFile();
                    String fileType = getFileExtension(originalFilename);
                    Document document = Document.builder()
                            .name(originalFilename)
                            .path(destinationFile.toString())
                            .type(fileType)
                            .size(file.length())
                            .build();
                    return documentRepository.save(document);
                }))
                .flatMap(savedDocument ->
                    // 3. Trigger indexing service
                    triggerIndexing(savedDocument.getPath())
                            .thenReturn(savedDocument) // Return the document after triggering
                )
                .map(DocumentResponse::from);
    }

    public Mono<Void> deleteById(Long documentId) {
        return documentRepository.findById(documentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Document not found")))
                .flatMap(document -> {
                    // 1. Delete physical file
                    File file = new File(document.getPath());
                    if (file.exists()) {
                        if (!file.delete()) {
                            log.warn("Failed to delete physical file: {}", document.getPath());
                        }
                    }
                    // 2. Trigger de-indexing service
                    return triggerDeindexing(document.getName())
                            .then(Mono.just(document)); // Pass document to the next step
                })
                .flatMap(document ->
                    // 3. Delete DB record
                    documentRepository.deleteById(document.getId())
                );
    }

    private Mono<Void> triggerIndexing(String filePath) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();
        record IndexingRequest(String filePath) {}

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(webClient.post()
                        .uri("http://indexing-service:8001/documents")
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .bodyValue(new IndexingRequest(filePath))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .doOnError(e -> log.error("Failed to trigger indexing service for file: {}", filePath, e))
                );
    }

    private Mono<Void> triggerDeindexing(String fileName) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(webClient.delete()
                        .uri("http://indexing-service:8001/documents/{fileName}", fileName)
                        .header("X-API-KEY", key)
                        .header("X-API-SECRET", secret)
                        .retrieve()
                        .bodyToMono(Void.class)
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
