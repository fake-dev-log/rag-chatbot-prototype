package prototype.coreapi.global.config;

import prototype.coreapi.global.exception.ErrorCode;

import prototype.coreapi.domain.auth.security.JwtAuthenticationWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import prototype.coreapi.global.response.WebfluxErrorResponseWriter;

import static prototype.coreapi.global.enums.Role.ADMIN;

/**
 * Configures the security settings for the Spring WebFlux application.
 * Enables method-level security and defines the security filter chain.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityWebfluxConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;
    private final CorsConfig corsConfig;
    private final WebfluxErrorResponseWriter webfluxErrorResponseWriter;

    /**
     * Configures the security filter chain for the application.
     * Disables CSRF, sets up stateless session management, configures exception handling,
     * defines authorization rules for various paths, and adds custom filters for CORS and JWT authentication.
     * 
     * @param http The ServerHttpSecurity object to configure security.
     * @return The configured SecurityWebFilterChain.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        
        http
                // Disable CSRF protection, as this is a stateless API that uses JWTs for auth.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Make the session management stateless. No session will be created or used.
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Configure custom exception handlers for authentication and authorization errors.
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((exchange, denied) ->
                                webfluxErrorResponseWriter.writeError(exchange, ErrorCode.FORBIDDEN)
                        )
                        .authenticationEntryPoint((exchange, authEx) ->
                                webfluxErrorResponseWriter.writeError(exchange, ErrorCode.AUTH_FAILED)
                        )
                )

                // Define authorization rules for specific paths.
                .authorizeExchange(authz -> authz
                        .pathMatchers("/auth/sign-in", "/auth/sign-up", "/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.HEAD, "/members/email-exists").permitAll()
                        .pathMatchers("/admin/**").hasRole(ADMIN.name())
                        .pathMatchers(HttpMethod.GET, "/common/health").permitAll()
                        .pathMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyExchange().authenticated() // All other requests must be authenticated.
                )

                // Add the custom CORS filter.
                .addFilterAt(
                        corsConfig.corsWebFilter(),
                        SecurityWebFiltersOrder.CORS
                )

                // Add the custom JWT authentication filter before the standard authentication processing.
                .addFilterAt(
                        jwtAuthenticationWebFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                );

        return http.build();
    }

    /**
     * Provides a BCryptPasswordEncoder bean for hashing passwords.
     * This is used for securely storing and verifying user passwords.
     * 
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        
        return new BCryptPasswordEncoder();
    }
}