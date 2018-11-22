package org.coinen.reactive.persistence.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.IOException;

@Data
public class ExternalStudyDto {
    private final static ObjectMapper om = new ObjectMapper();

    private double value;

    public static ExternalStudyDto fromString(String body) {
        try {
            return om.readValue(body, ExternalStudyDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}