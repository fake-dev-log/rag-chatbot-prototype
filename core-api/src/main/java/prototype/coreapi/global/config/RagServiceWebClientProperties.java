package prototype.coreapi.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag-service.api")
@Data
public class RagServiceWebClientProperties {
    private String baseUrl;
}
