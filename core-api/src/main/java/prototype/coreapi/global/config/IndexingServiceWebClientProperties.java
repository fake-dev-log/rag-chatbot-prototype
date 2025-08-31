package prototype.coreapi.global.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "indexing-service.api")
@Data
public class IndexingServiceWebClientProperties {
    private String baseUrl;
}
