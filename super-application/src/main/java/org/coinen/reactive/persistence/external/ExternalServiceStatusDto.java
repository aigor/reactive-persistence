package org.coinen.reactive.persistence.external;

import lombok.Data;

@Data
public class ExternalServiceStatusDto {
    private int activeRequests;
}
