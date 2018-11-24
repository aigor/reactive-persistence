package org.coinen.reactive.persistence.db;


import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorldPopDensityCouchbaseRepository
    extends ReactiveCouchbaseRepository<WorldPopDensityDto, String> {

    @Override
    Flux<WorldPopDensityDto> findAll();

    @Override
    Mono<WorldPopDensityDto> findById(@NonNull String id);
}
