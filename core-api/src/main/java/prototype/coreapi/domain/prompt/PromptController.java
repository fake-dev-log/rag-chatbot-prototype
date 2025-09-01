package prototype.coreapi.domain.prompt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import prototype.coreapi.domain.prompt.dto.PromptRequest;
import prototype.coreapi.domain.prompt.dto.PromptResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/prompts")
@Tag(name = "Prompt Management", description = "APIs for managing prompt templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class PromptController {

    private final PromptService promptService;

    @GetMapping
    @Operation(summary = "Get all prompt templates")
    public Flux<PromptResponse> getAllPrompts() {
        return promptService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single prompt template by ID")
    public Mono<PromptResponse> getPromptById(@PathVariable Long id) {
        return promptService.findById(id);
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Get a single prompt template by name")
    public Mono<PromptResponse> getPromptByName(@PathVariable String name) {
        return promptService.findByName(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new prompt template")
    public Mono<PromptResponse> createPrompt(@Valid @RequestBody PromptRequest request) {
        return promptService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing prompt template")
    public Mono<PromptResponse> updatePrompt(@PathVariable Long id, @Valid @RequestBody PromptRequest request) {
        return promptService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a prompt template")
    public Mono<Void> deletePrompt(@PathVariable Long id) {
        return promptService.deleteById(id);
    }

    @PostMapping("/{id}/apply")
    public Mono<String> applyPrompt(@PathVariable Long id) {
        return promptService.apply(id);
    }
}