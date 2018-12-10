package org.coinen.reactive.persistence;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.db.DatabaseFacade;
import org.coinen.reactive.persistence.db.EuropePopulationMongoRepository;
import org.coinen.reactive.persistence.db.UsSalesR2dbcRepository;
import org.coinen.reactive.persistence.db.WorldGdpCassandraRepository;
import org.coinen.reactive.persistence.db.WorldPopDensityCouchbaseRepository;
import org.coinen.reactive.persistence.db.jdbc.UsSalesJdbcRepository;
import org.coinen.reactive.persistence.external.ExternalService;
import org.coinen.reactive.persistence.utils.AppSchedulers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.function.DatabaseClient;
import org.springframework.data.r2dbc.function.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.http.HttpClient;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class AppConfiguration {
    static final String IO_WORKER_NAME = "ioWorker";

    // --- Services ------------------------------------------------------------

    @Bean
    public ExternalService externalService(
        @Value("${external.service.url}") String externalServiceUrl,
        HttpClient httpClient,
        WebClient webClient
    ) {
        return new ExternalService(externalServiceUrl, httpClient, webClient);
    }

    @Bean
    public DatabaseFacade databaseFacade(
        Scheduler ioScheduler,
        WorldPopDensityCouchbaseRepository worldPopDensityCouchbaseRepository,
        WorldGdpCassandraRepository worldGdpCassandraRepository,
        EuropePopulationMongoRepository europePopulationMongoRepository,
        UsSalesJdbcRepository usSalesJdbcRepository,
        UsSalesR2dbcRepository usSalesR2dbcRepository
    ) {
        return new DatabaseFacade(
            ioScheduler,
            worldGdpCassandraRepository,
            europePopulationMongoRepository,
            worldPopDensityCouchbaseRepository,
            usSalesJdbcRepository,
            usSalesR2dbcRepository
        );
    }

    // --- R2DBC configuration -------------------------------------------------
    @Bean
    public DatabaseClient databaseClient(
        @Value("${spring.datasource.url}") String url,
        @Value("${spring.datasource.username}") String user,
        @Value("${spring.datasource.password}") String password
    ) {
        // Parse database connection params into R2DBC friendly format
        var host = url.substring(url.indexOf("//") + 2, url.lastIndexOf(":"));
        var port = Integer.parseInt(url.substring(url.lastIndexOf(":") + 1, url.lastIndexOf("/")));
        var database = url.substring(url.lastIndexOf("/") + 1);

        return databaseClient(host, port, database, user, password);
    }

    private DatabaseClient databaseClient(
        String host,
        int port,
        String database,
        String user,
        String password
    ) {
        log.info("Reactive Postgres config. Host: '{}', DB: '{}', user: '{}'", host, database, user);

        var connectionFactory =
            new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(user)
                .password(password).build());

        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    public ReactiveDataAccessStrategy reactiveDataAccessStrategy() {
        return new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE);
    }

    @Bean
    public R2dbcRepositoryFactory factory(
        DatabaseClient client,
        ReactiveDataAccessStrategy reactiveDataAccessStrategy
    ) {
        var context = new RelationalMappingContext();
        context.afterPropertiesSet();

        return new R2dbcRepositoryFactory(client, context, reactiveDataAccessStrategy);
    }

    @Bean
    public UsSalesR2dbcRepository repository(R2dbcRepositoryFactory factory) {
        return factory.getRepository(UsSalesR2dbcRepository.class);
    }

    // --- Workers, http clients -----------------------------------------------

    @Bean("ioWorker")
    public ThreadPoolExecutor executor(@Value("${io.worker.size}") int ioWorkerSize){
        return AppSchedulers.newExecutor(IO_WORKER_NAME, ioWorkerSize);
    }

    @Bean("ioScheduler")
    public Scheduler ioScheduler(ThreadPoolExecutor executor) {
        return Schedulers.fromExecutor(executor);
    }

    @Bean
    public HttpClient httpClient(){
        return HttpClient.newBuilder().build();
    }

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }

}
