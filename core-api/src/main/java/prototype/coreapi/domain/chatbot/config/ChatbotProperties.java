package prototype.coreapi.domain.chatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag-service.api")
@Data
public class ChatbotProperties {
    private String baseUrl;
}
