/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.coinen.reactive.persistence;

import lombok.Value;

@Value
public class StudyResult {
    private final String colorSchema;
    private final Double colorValue;
    private final String pinValue;
}
