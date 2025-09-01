package prototype.coreapi.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import prototype.coreapi.global.util.OneTimeKeyAuthFilter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebClientFactory {

    private final Map<String, WebClient> webClientCache = new ConcurrentHashMap<>();

    private final WebClient.Builder webClientBuilder;
    private final RagServiceWebClientProperties ragServiceProps;
    private final IndexingServiceWebClientProperties indexingServiceProps;
    private final OneTimeKeyAuthFilter oneTimeKeyAuthFilter;

    public enum ServiceType {
        RAG, INDEXING
    }

    public WebClient getWebClient(ServiceType serviceType) {
        String serviceName = serviceType.name();

        return webClientCache.computeIfAbsent(serviceName, this::createWebClient);
    }

    private WebClient createWebClient(String serviceName) {
        ServiceType serviceType = ServiceType.valueOf(serviceName);

        WebClient.Builder builder = webClientBuilder.clone()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(oneTimeKeyAuthFilter);

        return switch (serviceType) {
            case RAG -> builder
                    .baseUrl(ragServiceProps.getBaseUrl())
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_NDJSON_VALUE)
                    .build();
            case INDEXING -> builder
                    .baseUrl(indexingServiceProps.getBaseUrl())
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();
        };
    }
}
