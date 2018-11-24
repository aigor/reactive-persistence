package org.coinen.reactive.persistence.db;


import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorldPopDensityCouchbaseRepository
    extends ReactiveCouchbaseRepository<WorldPopDensityDto, String> {

    // Does not work without view "/all"
    @Override
    Flux<WorldPopDensityDto> findAll();

    @Override
    Mono<WorldPopDensityDto> findById(@NonNull String id);

    @Query("SELECT META(`bucket1`).id AS _ID, META(`bucket1`).cas AS _CAS, `bucket1`.*" +
           " FROM bucket1 " +
           " WHERE density < 1000")
    Flux<WorldPopDensityDto> findByDensityLessThan10000();
}
