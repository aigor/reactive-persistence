/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.coinen.reactive.persistence.utils;

import org.coinen.reactive.persistence.StudyRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

public final class Utils {
    private Utils() { }

    public static StudyRequest parseRequest(ServerRequest request) {
        String study = request.pathVariable("study");
        String region = request.pathVariable("region");
        String timeout = request.queryParam("timeout").orElse(null);
        return new StudyRequest(study, region, timeout);
    }
}
