package prototype.coreapi.global.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import prototype.coreapi.global.exception.BusinessException;
import prototype.coreapi.global.exception.ErrorCode;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class WebfluxErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public Mono<Void> writeError(ServerWebExchange exchange, ErrorCode code) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(code.getCode()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<?> resp = RestResponse.customError(new BusinessException(code));
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsString(resp).getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            // Fallback in case serialization fails
            bytes = ("{\"error\":\"" + code.name() + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
