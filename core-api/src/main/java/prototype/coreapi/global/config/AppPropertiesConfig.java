package prototype.coreapi.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        RagServiceWebClientProperties.class,
        IndexingServiceWebClientProperties.class
})
public class AppPropertiesConfig {
}
