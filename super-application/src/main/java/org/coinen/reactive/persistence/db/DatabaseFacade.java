package org.coinen.reactive.persistence.db;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.db.jdbc.UsSalesJdbcRepository;
import org.coinen.reactive.persistence.model.StudyRequestDto;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.between;
import static java.time.Instant.now;

@Slf4j
@RequiredArgsConstructor
public class DatabaseFacade {
    private final Random rnd = new Random();

    private final boolean batchedRequest = false;
    private final int latency = 2500;

    // IO scheduler for executing blocking requests
    private final Scheduler ioScheduler;

    // Repositories for different DBs
    private final WorldGdpCassandraRepository worldGdpCassandraRepository;
    private final EuropePopulationMongoRepository europePopulationMongoRepository;
    private final WorldPopDensityCouchbaseRepository worldPopDensityCouchbaseRepository;
    private final UsSalesJdbcRepository usSalesJdbcRepository;
    private final UsSalesR2dbcRepository usSalesR2dbcRepository;


    public Mono<Object> resolvePersistedData(StudyRequestDto request) {
        switch (request.getStudy()) {
            case "world-gdp":
                return withTiming(worldGdpCassandra(request.getRegion()), "Cassandra");
            case "europe-pop":
                return withTiming(europePopulationMongo(request.getRegion()), "Mongo");
            case "world-pop-dens":
                return withTiming(worldPopDensityCouch(request.getRegion()), "Couchbase");
            case "usa-districts-jdbc":
                return withTiming(usSalesJdbc(request.getRegion()), "JDBC");
            case "usa-districts-r2dbc":
                return withTiming(usSalesR2Dbc(request.getRegion()), "R2DBC");
            case "usa-districts-all-blocking":
                return withTiming(usSalesR2Dbc(request.getRegion()), "JDBC");
            default:
                log.info("Have no datasource for study '{}', returning random data", request.getStudy());
                return Mono.delay(Duration.ofSeconds(2))
                    .map(__ -> randomDoubleValue());
        }
    }

    private Mono<Object> worldPopDensityCouch(String region) {
        if (batchedRequest) {
            return worldPopDensityCouchbaseRepository
                .findAll()
                //.doOnNext(r -> log.debug(" [Mongo -> App]: {}", r))
                .filter(dto -> region.equals(dto.getId()))
                .next()
                .map(WorldPopDensityDto::getDensity);
        } else {
            return worldPopDensityCouchbaseRepository
                .findById(region)
                .map(WorldPopDensityDto::getDensity);
        }
    }

    private Mono<Object> europePopulationMongo(String region) {
        if (batchedRequest) {
            return europePopulationMongoRepository
                .findAll()
                //.doOnNext(r -> log.debug(" [Mongo -> App]: {}", r))
                .filter(dto -> region.equals(dto.getCode()))
                .next()
                .map(EuropePopulationDto::getPopulation);
        } else {
            return europePopulationMongoRepository
                .findByCodeWithLatency(region, latency)
                .map(EuropePopulationDto::getPopulation);
        }
    }

    private Mono<Object> worldGdpCassandra(String region) {
        if (batchedRequest) {
            return worldGdpCassandraRepository
                .findAll()
                //.doOnNext(r -> log.debug(" [Cassandra -> App]: {}", r))
                .filter(dto -> region.equals(dto.getCountry_code()))
                .next()
                .map(WorldGdpDto::getGdp);
        } else {
            return worldGdpCassandraRepository
                .findById(region)
                .map(WorldGdpDto::getGdp);
        }
    }

    private Mono<Object> usSalesJdbc(String region) {
        return Mono.<Object>fromCallable(() ->
            usSalesJdbcRepository
            .findById(region)
            .map(UsSalesDataDto::getSales)
            .orElse(null)
        ).subscribeOn(ioScheduler);
    }

    private Mono<Object> usSalesR2Dbc(String region) {
        // Used for batched & non-batched mode
        return usSalesR2dbcRepository
            .findAll()
            .filter(data -> region.equals(data.getCode()))
            .next()
            .map(UsSalesDataDto::getSales);
    }

    private double randomDoubleValue() {
        return rnd.nextDouble() * 1000;
    }

    private static <T> Mono<T> withTiming(Mono<T> publisher, String repoType) {
        AtomicReference<Instant> startTime = new AtomicReference<>();
        return publisher
            .doOnSubscribe(__ -> startTime.set(now()))
            .doOnError(e -> log.warn("Error: ", e))
            .doFinally(__ ->
                log.debug("{} request finished, took: {}",
                    repoType, between(startTime.get(), now())));
    }
}
