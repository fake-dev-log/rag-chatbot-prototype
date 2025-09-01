package prototype.coreapi.global.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import prototype.coreapi.global.redis.OneTimeKeyStoreProvider;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OneTimeKeyAuthFilter implements ExchangeFilterFunction {

    private final OneTimeKeyStoreProvider oneTimeKeyStoreProvider;

    @Override
    @NonNull
    public Mono<ClientResponse> filter(@NonNull ClientRequest request, @NonNull ExchangeFunction next) {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();

        return oneTimeKeyStoreProvider.save(key, secret)
                .then(Mono.defer(() -> {
                    ClientRequest newRequest = ClientRequest.from(request)
                            .header("X-API-KEY", key)
                            .header("X-API-SECRET", secret)
                            .build();
                    return next.exchange(newRequest);
                }));
    }
}
