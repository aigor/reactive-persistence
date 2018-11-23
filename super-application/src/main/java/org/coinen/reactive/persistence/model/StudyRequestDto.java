package org.coinen.reactive.persistence.model;

import lombok.Value;

@Value
public class StudyRequestDto {
    private final String study;
    private final String region;
    private final String timout;
}
