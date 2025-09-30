package prototype.coreapi.global.filter;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1) // Execute this filter first
public class RequestLoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    @NonNull
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().toString();

        logger.info("Request started: {} {}", method, path);

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            String statusToLog = (statusCode != null) ? String.valueOf(statusCode.value()) : "[NO_STATUS]";
            logger.info("Request finished: {} {} | Status: {} | Duration: {}ms", method, path, statusToLog, duration);
        });
    }
}
