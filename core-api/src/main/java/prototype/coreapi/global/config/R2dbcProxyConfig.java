package prototype.coreapi.global.config;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

@Configuration
@Slf4j
public class R2dbcProxyConfig {

    @Bean
    @Primary
    public ConnectionFactory connectionFactory(R2dbcProperties props) {
        ConnectionFactoryOptions baseOpts = ConnectionFactoryOptions.parse(props.getUrl());

        ConnectionFactoryOptions opts = ConnectionFactoryOptions.builder()
                .from(baseOpts)
                .option(USER, props.getUsername())
                .option(PASSWORD, props.getPassword())
                .build();

        ConnectionFactory actual = ConnectionFactories.get(opts);

        return ProxyConnectionFactory.builder(actual)
                .onAfterQuery(exec ->
                        log.info(QueryExecutionInfoFormatter.showAll().format(exec))
                )
                .build();
    }
}
