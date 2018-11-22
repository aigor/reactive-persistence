/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.coinen.reactive.persistence;

import lombok.Value;

@Value
public class StudyRequest {
    private final String study;
    private final String region;
    private final String timout;
}
