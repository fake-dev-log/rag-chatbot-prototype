package prototype.coreapi.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "development"})
@OpenAPIDefinition(
        info = @Info(
                title = "CORE API",
                description = "RAG 챗봇 프로토타입 프로젝트 API 명세서",
                version = "v1.0"
        )
)
public class SwaggerConfig {

    @Value("${springdoc.swagger-ui.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI CoreAPIOpenAPI() {
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components().addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("Bearer"));

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .addServersItem(new Server()
                        .url(serverUrl)
                        .description("RAG Chatbot prototype Core API"))
                .components(components);
    }

    @Bean
    public GroupedOpenApi ServiceApi() {
        return GroupedOpenApi.builder()
                .group("RAG Chatbot Prototype")
                .displayName("Core API")
                .pathsToMatch("/**")
                .build();
    }
}
