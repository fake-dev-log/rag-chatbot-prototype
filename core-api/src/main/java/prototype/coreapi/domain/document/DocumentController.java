package prototype.coreapi.domain.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import prototype.coreapi.domain.document.dto.DocumentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "문서 관리", description = "RAG 문서 관리 API")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "문서 목록 조회", description = "RAG에 사용되는 모든 문서를 조회합니다.")
    public Flux<DocumentResponse> getDocuments() {
        return documentService.findAll();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "문서 업로드", description = "새로운 문서를 업로드하고 인덱싱을 요청합니다.")
    public Mono<DocumentResponse> uploadDocument(
            @RequestPart("file") Mono<FilePart> filePartMono
    ) {
        return filePartMono.flatMap(documentService::upload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "문서 삭제", description = "기존 문서를 삭제하고 인덱스에서 제거를 요청합니다.")
    public Mono<Void> deleteDocument(@PathVariable("id") Long documentId) {
        return documentService.deleteById(documentId);
    }
}
