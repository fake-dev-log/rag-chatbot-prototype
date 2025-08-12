package prototype.coreapi.domain.chatbot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(ChatbotProperties.class)
public class ChatbotConfig {

    @Bean
    public WebClient chatbotWebClient(WebClient.Builder builder,
                                      ChatbotProperties props) {
        // 필요하면 로깅ㆍ모니터링 필터 추가
        return builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_NDJSON_VALUE)
                .filter(ExchangeFilterFunction.ofRequestProcessor(Mono::just))
                .build();
    }
}
