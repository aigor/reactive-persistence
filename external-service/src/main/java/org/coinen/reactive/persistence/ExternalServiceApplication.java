package org.coinen.reactive.persistence;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static java.time.Duration.ofMillis;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * TODO: Should return something useful, like temperature in a city
 */
@Slf4j
@SpringBootApplication
public class ExternalServiceApplication {
	private static final int DEFAULT_TIMEOUT = 1000;

	public static void main(String[] args) {
		SpringApplication.run(ExternalServiceApplication.class, args);
	}

	private final AtomicInteger activeRequests = new AtomicInteger(0);

	@Bean
	public RouterFunction<ServerResponse> routerFunction() {
		return RouterFunctions
			.route(
				GET("/service/{timeout}"),
				request -> ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(
						Mono.delay(getDelay(request))
							.map(__ -> PriceDto.random())
							.doOnSubscribe(__ -> {
								activeRequests.incrementAndGet();
								log.debug("Starting request processing");
							})
							.doFinally(__ -> {
								activeRequests.decrementAndGet();
								log.debug("Request processing finished");
							}),
						PriceDto.class)
			).andRoute(
				GET("/status"),
				request -> ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(applicationStatus(), AppStatusDto.class)
			);
	}

	private Flux<AppStatusDto> applicationStatus() {
		return Flux.interval(Duration.ofMillis(250))
			.map(__ -> new AppStatusDto(activeRequests.get()));
	}

	private Duration getDelay(ServerRequest req) {
		try {
			return ofMillis(parseInt(req.pathVariable("timeout")));
		} catch (Exception e) {
			return ofMillis(DEFAULT_TIMEOUT);
		}
	}

	@Value
	public static class PriceDto {
		private static Random rnd = new Random();

		private final double value;

		static PriceDto random() {
			return new PriceDto(rnd.nextDouble() * 1000);
		}
	}

	@Value
	public static class AppStatusDto {
		private final int activeRequests;
	}
}
