package org.coinen.reactive.persistence.utils;

import org.coinen.reactive.persistence.external.ExternalServiceMetricsDto;
import org.coinen.reactive.persistence.model.AppStatusDto;

import java.util.concurrent.ThreadPoolExecutor;

public final class MonitoringUtils {
    private MonitoringUtils() { }

    public static AppStatusDto toAppStatus(
        ThreadPoolExecutor executor,
        int activeRequests,
        ExternalServiceMetricsDto externalStatus
    ) {
        return new AppStatusDto(
            executor.getMaximumPoolSize(),
            executor.getActiveCount(),
            executor.getQueue().size(),
            activeRequests,
            externalStatus.getActiveRequests()
        );
    }
}
