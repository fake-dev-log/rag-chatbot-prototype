package prototype.coreapi.domain.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/admin/documents")
@Tag(name = "Document Management", description = "APIs for managing documents")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final SseEmitterService sseEmitterService; // Inject SSE Service

    @GetMapping
    @Operation(summary = "Get all documents")
    public Flux<DocumentResponse> getDocuments() {
        return documentService.findAll();
    }

    @GetMapping("/status-stream")
    @Operation(summary = "Stream document status updates", description = "Connect to receive real-time updates on document indexing status.")
    public Flux<ServerSentEvent<DocumentResponse>> getDocumentStatusStream() {
        return sseEmitterService.connect();
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all distinct document categories")
    public Mono<List<String>> getCategories() {
        return documentService.findAllCategories();
    }


    /**
     * Uploads a new document to the system. The document will be stored and then indexed.
     * @param filePartMono A Mono emitting the uploaded file.
     * @return A Mono emitting the DocumentResponse for the uploaded document.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload Document", description = "Uploads a new document and requests indexing.")
    public Mono<DocumentResponse> uploadDocument(
            @RequestPart("file") Mono<FilePart> filePartMono,
            @RequestPart("category") String category
    ) { 
        return filePartMono.flatMap(file -> documentService.upload(file, category));
    }

    /**
     * Deletes a document from the system by its ID. This also triggers de-indexing.
     * @param documentId The ID of the document to delete.
     * @return A Mono<Void> indicating completion of the deletion operation.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete document", description = "Deletes an existing document and requests its removal from the index.")
    public Mono<Void> deleteDocument(@PathVariable("id") Long documentId) {
        return documentService.deleteById(documentId);
    }
}
