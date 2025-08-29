package prototype.coreapi.domain.auth.security;

import prototype.coreapi.domain.auth.dto.LoginPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import prototype.coreapi.global.exception.ErrorCode;
import prototype.coreapi.global.redis.TokenBlacklistStoreProvider;
import prototype.coreapi.global.response.WebfluxErrorResponseWriter;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final List<String> AUTH_WHITELIST = List.of(
            "/auth/sign-in",
            "/auth/sign-up",
            "/auth/refresh"
    );

    private final JwtService jwtService;
    private final TokenBlacklistStoreProvider blacklist;
        private final WebfluxErrorResponseWriter webfluxErrorResponseWriter;

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        final String path = exchange.getRequest().getURI().getPath();

        if (AUTH_WHITELIST.contains(path)) {
            return chain.filter(exchange);
        }

        final String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        final String token = authHeader.substring(7);

        return blacklist.isBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return webfluxErrorResponseWriter.writeError(exchange, ErrorCode.INVALID_TOKEN);
                    }

                    try {
                        if (!jwtService.isValidToken(token)) {
                            return webfluxErrorResponseWriter.writeError(exchange, ErrorCode.INVALID_TOKEN);
                        }

                        Long memberId = jwtService.extractMemberId(token);
                        List<GrantedAuthority> authorities = jwtService.extractRoles(token).stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        LoginPrincipal principal = new LoginPrincipal(memberId);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(principal, null, authorities);

                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

                    } catch (ExpiredJwtException e) {
                        return webfluxErrorResponseWriter.writeError(exchange, ErrorCode.EXPIRED_TOKEN);
                    } catch (JwtException e) {
                        return webfluxErrorResponseWriter.writeError(exchange, ErrorCode.INVALID_TOKEN);
                    } catch (Exception e) {
                        return webfluxErrorResponseWriter.writeError(exchange, ErrorCode.SERVICE_FAIL);
                    }
                });
    }

    
}

