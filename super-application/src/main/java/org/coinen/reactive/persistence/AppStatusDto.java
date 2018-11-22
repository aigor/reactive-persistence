package org.coinen.reactive.persistence;

import lombok.Value;

@Value
public class AppStatusDto {
    private final int poolSize;
    private final int poolUsed;
    private final int poolQueueSize;
    private final int activeRequests;
    private final int externalServiceActiveRequests;
}
