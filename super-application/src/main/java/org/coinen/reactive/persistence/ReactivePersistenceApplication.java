package org.coinen.reactive.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.coinen.reactive.persistence.db.DatabaseFacade;
import org.coinen.reactive.persistence.external.ExternalService;
import org.coinen.reactive.persistence.external.ExternalStudyDto;
import org.coinen.reactive.persistence.model.AppStatusDto;
import org.coinen.reactive.persistence.model.StudyRequestDto;
import org.coinen.reactive.persistence.model.StudyResultDto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.coinen.reactive.persistence.AppConfiguration.IO_WORKER_NAME;
import static org.coinen.reactive.persistence.utils.MonitoringUtils.toAppStatus;
import static org.coinen.reactive.persistence.utils.SerializationUtils.parseRequest;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.resources;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;


// Done: Put data into Postgres
// Done: Repository for loading data form JDBC Postgres
// Done: Repository for loading data form R2DBC Postgres
// Done: Put data into Cassandra
// Done: Repository for loading data form Cassandra
// Done: Put data into Mongo
// Done: Repository for loading data form Mongo
// Done: Put data into CouchBase
// Done: Repository for loading data form CouchBase
// TODO: Dedicated ADBA example
// TODO: Dedicated R2DBC example
// TODO: Finish slides
// TODO: UI to call for all states on hot key
// TODO: Pictures for layers of Cassandra/Mongo/etc...

@EnableJdbcRepositories("org.coinen.reactive.persistence.db.jdbc")
@EnableR2dbcRepositories
@RequiredArgsConstructor
@Slf4j
@SpringBootApplication
public class ReactivePersistenceApplication implements CommandLineRunner {

	// Services
	private final ThreadPoolExecutor executor;
	private final Scheduler ioScheduler;
	private final DatabaseFacade dbFacade;
	private final ExternalService externalService;

	// Statistics
	private final AtomicInteger activeRequests = new AtomicInteger(0);

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
					.body(
						withMetrics(processRequestBlocking(parseRequest(request))),
						StudyResultDto.class)
			).andRoute(
				GET("/nio/service/{study}/{region}"),
				request -> ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(
						withMetrics(processRequestReactive(parseRequest(request))),
						StudyResultDto.class)
            ).andRoute(
                GET("/status"),
                request -> ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(applicationStatus(), AppStatusDto.class)
			).andOther(
				resources("/**", new ClassPathResource("/static"))
			);
	}

	// --- Blocking handling ---------------------------------------------------

    private Mono<StudyResultDto> processRequestBlocking(StudyRequestDto studyRequest) {
		if ("uk-sync".equals(studyRequest.getStudy()) || "uk-async".equals(studyRequest.getStudy())) {
			return Mono.fromCallable(
				() -> {
					ExternalStudyDto externalStudyDto = externalService.syncRequest(studyRequest);
					return StudyResultDto.temperature(externalStudyDto.getValue());
				})
				.publishOn(ioScheduler);
		} else {
			return Mono.zip(
				Mono.fromCallable(() -> externalService.syncRequest(studyRequest))
					.publishOn(ioScheduler),
				Mono.fromCallable(() -> dbFacade.resolvePersistedData(studyRequest).block())
					.publishOn(ioScheduler),
				(external, persisted) ->
					StudyResultDto.generic(external.getValue(), persisted)
			);
		}
	}

	// ---- Async handling -----------------------------------------------------

	private Mono<StudyResultDto> processRequestReactive(StudyRequestDto studyRequest) {
		if ("uk-sync".equals(studyRequest.getStudy()) || "uk-async".equals(studyRequest.getStudy())) {
			return externalService.reactiveRequest(studyRequest)
				.map(externalData -> StudyResultDto.temperature(externalData.getValue()));
		} else {
			return Mono.zip(
				externalService.reactiveRequest(studyRequest),
				dbFacade.resolvePersistedData(studyRequest),
				(external, persisted) -> StudyResultDto.generic(external.getValue(), persisted)
			).doOnError(e -> log.warn("Error:", e));
		}
	}

	// --- App's metrics -------------------------------------------------------
	private Flux<AppStatusDto> applicationStatus() {
		return Flux.combineLatest(
			Flux.interval(Duration.ofMillis(250)),
			externalService.serviceStatus(),
			(__, externalStatus) ->
				toAppStatus(executor, activeRequests.get(), externalStatus)
		);
	}

	private Mono<StudyResultDto> withMetrics(Mono<StudyResultDto> stream) {
		return stream
			.doOnSubscribe(s -> activeRequests.incrementAndGet())
			.doFinally(s -> activeRequests.decrementAndGet());
	}

	@Override
	public void run(String... args) {
		Flux.interval(Duration.ofSeconds(1))
			.doOnEach(i -> log.debug("[{} status] active req: {}, run/max: {}/{}, queued tasks: {}",
				IO_WORKER_NAME,
				activeRequests.get(),
				executor.getActiveCount(),
				executor.getMaximumPoolSize(),
				executor.getQueue().size()))
		//	.subscribe()
		;
	}
}
