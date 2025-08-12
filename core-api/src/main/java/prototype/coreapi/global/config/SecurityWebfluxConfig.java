package prototype.coreapi.global.config;

import prototype.coreapi.domain.auth.security.JwtAuthenticationWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import static prototype.coreapi.global.enums.Role.ADMIN;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityWebfluxConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;
    private final CorsConfig corsConfig;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // SecurityContext를 저장할 리포지토리 자체를 NoOp으로 설정 → stateless
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // 인증/인가 예외 처리: Mono<Void>를 리턴하도록 setComplete() 호출
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                        .authenticationEntryPoint((exchange, authEx) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                )

                // 경로별 권한 설정
                .authorizeExchange(authz -> authz
                        .pathMatchers("/auth/sign-in", "/auth/sign-up", "/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.HEAD, "/members/email-exists").permitAll()
                        .pathMatchers("/admin/**").hasRole(ADMIN.name())
                        .pathMatchers(HttpMethod.GET, "/common/health").permitAll()
                        .pathMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(
                        corsConfig.corsWebFilter(),
                        SecurityWebFiltersOrder.CORS
                )

                // JWT 인증 WebFilter 등록
                .addFilterAt(
                        jwtAuthenticationWebFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
