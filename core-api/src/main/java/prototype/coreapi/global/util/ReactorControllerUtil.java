package prototype.coreapi.global.util;


import org.springframework.stereotype.Component;
import prototype.coreapi.global.exception.BusinessException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class ReactorControllerUtil {

    public <T> Mono<T> authorize(Mono<T> source,
                                 Predicate<T> permissionCheck,
                                 BusinessException forbidden) {
        return source
                .filter(permissionCheck)
                .switchIfEmpty(Mono.error(forbidden));
    }

    public <T,R> Mono<List<R>> collectListFlux(Flux<T> source,
                                               Function<T,R> mapper) {
        return source
                .map(mapper)
                .collectList();
    }
}
