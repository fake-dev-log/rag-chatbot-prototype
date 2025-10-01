package prototype.coreapi.domain.document;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.document.dto.StatusUpdateRequest;
import prototype.coreapi.domain.document.entity.Document;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/documents")
@RequiredArgsConstructor
public class DocumentInternalController {

    private final DocumentService documentService;

    @PatchMapping("/{id}/status")
    public Mono<Document> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return documentService.updateStatus(id, request.status());
    }
}
