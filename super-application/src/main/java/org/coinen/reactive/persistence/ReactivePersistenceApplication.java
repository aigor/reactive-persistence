package org.coinen.reactive.persistence;

import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.external.ExternalService;
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
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.coinen.reactive.persistence.utils.Utils.parseRequest;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@SpringBootApplication
public class ReactivePersistenceApplication implements CommandLineRunner {
	private static final String IO_WORKER = "ioWorker";

	private final ThreadPoolExecutor executor = AppSchedulers.newExecutor(IO_WORKER, 4);
	private final Scheduler ioScheduler = Schedulers.fromExecutor(executor);

	private final ExternalService externalService = new ExternalService(
		HttpClient.newBuilder().build(),
		WebClient.builder().build()
	);

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
					.body(processRequestBlocking(parseRequest(request)), StudyResult.class)
			).andRoute(
				GET("/nio/service/{study}/{region}"),
				request -> ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(processRequestReactive(parseRequest(request)), StudyResult.class)
            ).andRoute(
                GET("/status"),
                request -> ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(applicationStatus(), AppStatusDto.class)
			).andOther(
				resources("/**", new ClassPathResource("/static"))
			);
	}

    private Mono<StudyResult> processRequestBlocking(StudyRequest request) {
		return Mono.fromCallable(
			() -> {
				ExternalStudyDto externalStudyDto = externalService.syncRequest(request);
				return getFinalStudyResult(request.getStudy(), request.getRegion(), externalStudyDto);
			})
			.publishOn(ioScheduler)
			.doOnSubscribe(s -> activeIncomingRequests.incrementAndGet())
			.doFinally(s -> activeIncomingRequests.decrementAndGet());
	}

	private Mono<StudyResult> processRequestReactive(StudyRequest request) {
		return externalService
			.reqctiveRequest(request)
			.map(external ->
				getFinalStudyResult(request.getStudy(), request.getRegion(), external))
            .doOnSubscribe(s -> activeIncomingRequests.incrementAndGet())
            .doFinally(s -> activeIncomingRequests.decrementAndGet());
	}

	private StudyResult getFinalStudyResult(String study, String region, ExternalStudyDto externalStudyDto) {
		if ("uk-sync".equals(study) || "uk-async".equals(study)) {
			return new StudyResult("temperature", externalStudyDto.getValue(), null);
		} else {
			// TODO: Make some DB request
			return new StudyResult("green", externalStudyDto.getValue(), region);
		}
	}

	private Flux<AppStatusDto> applicationStatus() {
		return Flux.combineLatest(
			Flux.interval(Duration.ofMillis(250)),
			externalService.serviceStatus(),
			(__, externalStatus) -> new AppStatusDto(
				executor.getMaximumPoolSize(),
				executor.getActiveCount(),
				executor.getQueue().size(),
				activeIncomingRequests.get(),
				externalStatus.getActiveRequests()
			))
			.doOnError(e -> log.warn("Error on status", e));
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
}
