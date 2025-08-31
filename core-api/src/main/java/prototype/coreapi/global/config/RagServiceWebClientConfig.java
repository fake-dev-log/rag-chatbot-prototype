package prototype.coreapi.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(RagServiceWebClientProperties.class)
public class RagServiceWebClientConfig {

    @Bean
    public WebClient chatbotWebClient(WebClient.Builder builder,
                                      RagServiceWebClientProperties props) {
        // Add logging/monitoring filters if necessary
        return builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_NDJSON_VALUE)
                .build();
    }
}