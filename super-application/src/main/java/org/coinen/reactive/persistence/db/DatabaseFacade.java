package org.coinen.reactive.persistence.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.model.StudyRequestDto;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Random;


@Slf4j
@RequiredArgsConstructor
public class DatabaseFacade {
    private final Random rnd = new Random();

    // IO scheduler for executing blocking requests
    private final Scheduler ioScheduler;

    // Repositories for different DBs
    private final UsDistrictsSalesRepository usDistrictsSalesJdbcRepository;


    public Mono<Double> resolvePersistedData(StudyRequestDto request) {
        switch (request.getStudy()) {
            case "usa-districts-jdbc":
                return usSalesJdbc(request.getRegion());
            default:
                log.info("Have no datasource for study '{}', returning random data", request.getStudy());
                return Mono.delay(Duration.ofSeconds(2))
                    .map(__ -> randomDoubleValue());
        }
    }

    private Mono<Double> usSalesJdbc(String region) {
        return Mono.fromCallable(() ->
            usDistrictsSalesJdbcRepository
            .findById(region)
            .map(UsSalesDataDto::getSales)
            .orElse(null)
        ).subscribeOn(ioScheduler);
    }

    private double randomDoubleValue() {
        return rnd.nextDouble() * 1000;
    }
}
