package prototype.coreapi.domain.common;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
@Tag(name = "Common", description = "APIs for common functionalities")
public class CommonController {

    @GetMapping("/health")
    public Mono<Void> health() {
        return Mono.empty();
    }
}