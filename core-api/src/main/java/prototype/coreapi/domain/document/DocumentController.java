package prototype.coreapi.domain.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing documents within the RAG system.
 * Provides endpoints for retrieving, uploading, and deleting documents.
 * Access to these endpoints is restricted to users with the 'ADMIN' role.
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "RAG Document Management API")
@PreAuthorize("hasRole('ADMIN')")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Retrieves a list of all documents currently managed by the system.
     * @return A Flux of DocumentResponse containing details of each document.
     */
    @GetMapping
    @Operation(summary = "Retrieve document list", description = "Retrieves all documents used in RAG.")
    public Flux<DocumentResponse> getDocuments() { 
        return documentService.findAll();
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
            @RequestPart("file") Mono<FilePart> filePartMono
    ) { 
        return filePartMono.flatMap(documentService::upload);
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
