package org.coinen.reactive.persistence.db;


import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EuropePopulationMongoRepository
    extends ReactiveMongoRepository<EuropePopulationDto, ObjectId> {

    @Override
    Flux<EuropePopulationDto> findAll();

    Mono<EuropePopulationDto> findByCode(@NonNull String code);

    @Query("{ 'code' : ?0, $where: 'sleep(?1) || true' }")
    Mono<EuropePopulationDto> findByCodeWithLatency(@NonNull String code, int latency);
}
