/**
 * Copyright (C) Zoomdata, Inc. 2012-2018. All rights reserved.
 */
package org.coinen.reavive.http.client;

import lombok.extern.slf4j.Slf4j;
import reactor.adapter.JdkFlowAdapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Slf4j
public class HttClientExample {
    public static void main(String... args) throws InterruptedException {
        HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)  // this is the default
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://github.com/"))
            .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofPublisher())
            .thenAccept(httpResponse ->
                JdkFlowAdapter
                    .flowPublisherToFlux(httpResponse.body())
                    .subscribe(
                        bb -> {
                            log.info("New data chunk of size: {}", bb.size());
                            bb.forEach(buffer ->
                                log.info("{}" + StandardCharsets.UTF_8.decode(buffer)
                                    .toString()));
                        },
                        e -> log.warn("Error: {}", e),
                        () -> log.info("Transfer complete")
                    )
            );

        Thread.sleep(5000);
    }
}
