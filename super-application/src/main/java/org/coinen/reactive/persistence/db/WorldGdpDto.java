package org.coinen.reactive.persistence.db;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Table("world_gdp")
public class WorldGdpDto {
    @Id
    private String country_code;
    private double gdp;
}
