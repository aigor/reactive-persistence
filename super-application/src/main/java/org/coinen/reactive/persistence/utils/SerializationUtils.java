package org.coinen.reactive.persistence.utils;

import org.coinen.reactive.persistence.model.StudyRequestDto;
import org.springframework.web.reactive.function.server.ServerRequest;

public final class SerializationUtils {
    private SerializationUtils() { }

    public static StudyRequestDto parseRequest(ServerRequest request) {
        String study = request.pathVariable("study");
        String region = request.pathVariable("region");
        String timeout = request.queryParam("timeout").orElse(null);
        return new StudyRequestDto(study, region, timeout);
    }
}
