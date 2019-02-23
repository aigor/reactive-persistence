package org.coinen.reactive.persistence.db;


import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorldGdpCassandraRepository
    extends ReactiveCassandraRepository<WorldGdpDto, String> {

    @Override
    Flux<WorldGdpDto> findAll();

    @Override
    Mono<WorldGdpDto> findById(@NonNull String country_code);
}
