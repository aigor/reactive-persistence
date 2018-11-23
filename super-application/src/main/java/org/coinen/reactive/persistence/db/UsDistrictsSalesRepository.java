package org.coinen.reactive.persistence.db;


import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsDistrictsSalesRepository extends CrudRepository<UsSalesDataDto, String> {

    @Query("select * from us_sales_by_districts, pg_sleep(2) where code=:code")
    Optional<UsSalesDataDto> findById(@Param ("code") @NonNull String code);
}
