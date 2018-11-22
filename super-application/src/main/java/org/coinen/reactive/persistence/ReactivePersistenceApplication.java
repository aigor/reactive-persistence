package org.coinen.reactive.persistence;

import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.external.ExternalServiceStatusDto;
import org.coinen.reactive.persistence.external.ExternalStudyDto;
import org.coinen.reactive.persistence.utils.AppSchedulers;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@SpringBootApplication
public class ReactivePersistenceApplication implements CommandLineRunner {
	private static final String IO_WORKER = "ioWorker";
	private static final String EXTERNAL_SERVICE = "http://localhost:9090";

	private final ThreadPoolExecutor executor = AppSchedulers.newExecutor(IO_WORKER, 4);
	private final Scheduler ioScheduler = Schedulers.fromExecutor(executor);

	private final HttpClient httpClient = HttpClient.newBuilder().build();
	private final WebClient webClient = WebClient.builder().build();

	private final AtomicInteger activeIncomingRequests = new AtomicInteger(0);

	public static void main(String[] args) {
		SpringApplication.run(ReactivePersistenceApplication.class, args);
	}

	@Bean
	public RouterFunction<?> routerFunction() {
		return RouterFunctions
			.route(
				GET("/"),
				request -> ok().render(
					"index",
					Rendering.view("index"))
			).andRoute(
				GET("/service/{study}/{region}"),
				request -> ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(processRequestBlocking(request), StudyResult.class)
			).andRoute(
				GET("/nio/service/{study}/{region}"),
				request -> ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(processRequestReactive(request), StudyResult.class)
            ).andRoute(
                GET("/status"),
                request -> ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(applicationStatus(), AppStatusDto.class)
			).andOther(
				resources("/**", new ClassPathResource("/static"))
			);
	}

    private Flux<AppStatusDto> applicationStatus() {
	    return Flux.combineLatest(
            Flux.interval(Duration.ofMillis(250)),
            webClient.get()
                .uri(URI.create(EXTERNAL_SERVICE + "/status"))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .flatMapMany(response -> response.bodyToFlux(ExternalServiceStatusDto.class)),
            (__, externalStatus) -> new AppStatusDto(
                executor.getMaximumPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                activeIncomingRequests.get(),
                externalStatus.getActiveRequests()
            ))
            .doOnError(e -> log.warn("Error on status", e));
    }

    private Mono<StudyResult> processRequestBlocking(ServerRequest request) {
		String study = request.pathVariable("study");
		String region = request.pathVariable("region");
		String timeout = request.queryParam("timeout").orElse(null);

		return Mono.fromCallable(
			() -> {
				Instant start = now();
				log.info("Starting external call");
				HttpRequest req = HttpRequest.newBuilder()
					.uri(externalServiceUri(study, region, timeout))
					.build();

				HttpResponse<String> response = httpClient
					.send(req, HttpResponse.BodyHandlers.ofString());
				log.info("External call finished in {}", between(start, now()));
				ExternalStudyDto externalStudyDto = ExternalStudyDto.fromString(response.body());
				return getFinalStudyResult(study, region, externalStudyDto);
			})
			.publishOn(ioScheduler)
			.doOnSubscribe(s -> activeIncomingRequests.incrementAndGet())
			.doFinally(s -> activeIncomingRequests.decrementAndGet());
	}

	private Mono<StudyResult> processRequestReactive(ServerRequest request) {
		String study = request.pathVariable("study");
		String region = request.pathVariable("region");
		String timeout = request.queryParam("timeout").orElse(null);

		return webClient
			.get()
			.uri(externalServiceUri(study, region, timeout))
			.exchange()
			.flatMap(rsp -> rsp.bodyToMono(String.class)
				.map(ExternalStudyDto::fromString)
				.map(external ->
					getFinalStudyResult(study, region, external)))
            .doOnSubscribe(s -> activeIncomingRequests.incrementAndGet())
            .doFinally(s -> activeIncomingRequests.decrementAndGet());
	}

	private StudyResult getFinalStudyResult(String study, String region, ExternalStudyDto externalStudyDto) {
		return new StudyResult("blue", externalStudyDto.getValue(), region);
	}

	@Override
	public void run(String... args) {
		Flux.interval(Duration.ofSeconds(1))
			.doOnEach(i -> log.debug("[{} status] active req: {}, run/max: {}/{}, queued tasks: {}",
				IO_WORKER,
				activeIncomingRequests.get(),
				executor.getActiveCount(),
				executor.getMaximumPoolSize(),
				executor.getQueue().size()))
			.subscribe();
	}

	private URI externalServiceUri(String study, String region, String timeout) {
		return URI.create(
			EXTERNAL_SERVICE +
				"/service/" +
				study + "/" +
				region +
				(timeout != null ? "?timeout=" + timeout : "")
		);
	}
}
