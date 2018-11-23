package org.coinen.reactive.persistence;

import org.coinen.reactive.persistence.db.DatabaseFacade;
import org.coinen.reactive.persistence.db.UsDistrictsSalesRepository;
import org.coinen.reactive.persistence.external.ExternalService;
import org.coinen.reactive.persistence.utils.AppSchedulers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.http.HttpClient;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AppConfiguration {
    static final String IO_WORKER_NAME = "ioWorker";
    private static final int IO_WORKER_SIZE = 4;

    // --- Services ------------------------------------------------------------

    @Bean
    public ExternalService externalService(HttpClient httpClient, WebClient webClient) {
        return new ExternalService(httpClient, webClient);
    }

    @Bean
    public DatabaseFacade databaseFacade(
        Scheduler ioScheduler,
        UsDistrictsSalesRepository usDistrictsSalesJdbcRepository
    ) {
        return new DatabaseFacade(ioScheduler, usDistrictsSalesJdbcRepository);
    }

    // --- Workers, http clients -----------------------------------------------

    @Bean("ioWorker")
    public ThreadPoolExecutor executor(){
        return AppSchedulers.newExecutor(IO_WORKER_NAME, IO_WORKER_SIZE);
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
