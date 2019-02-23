package org.coinen.reactive.persistence;

import lombok.RequiredArgsConstructor;

// TODO: Use this enum instead of hard-coded values
@RequiredArgsConstructor
public enum Studies {
    UKRAINE_WEATHR_4_SYNC("uk-sync"),
    UKRAINE_WEATHR_27_ASYNC("uk-async"),
    WORLD_GDP("world-gdp"),
    EUROPE_POP("europe-pop"),
    WORLD_POP_DENSITY("world-pop-des"),
    US_SALES_JDBC("usa-districts-jdbc"),
    US_DISTRICS_R2DBC("usa-districts-r2dbc"),
    US_DISTRICS_BLOCKING("usa-districts-all-blocking");

    private final String name;
}
