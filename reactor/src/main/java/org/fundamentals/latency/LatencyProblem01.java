package org.fundamentals.latency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.fundamentals.latency.SimpleCurl.fetch;

/**
 * Feature: Consume some REST God Services
 *
 * Background: Decimal representation of Zeus: `Zeus` = 122101117115
 *
 * Scenario: Consume the API in a Happy path case
 *     Given a list of REST API about Greek, Roman & Nordic
 *     When  call and retrieve all API info from the good list
 *     Then  filter by god starting with `n`
 *     And   convert the names into a decimal format
 *     And   sum
 *
 * Scenario: Consume API but greek service is slow
 *     Given a list of REST API about Greek, Roman & Nordic
 *     When  call and retrieve all API info from the good list
 *     Then  filter by god starting with `n`
 *     And   convert the names into a decimal format
 *     And   sum
 *
 * Notes:
 * Every connection with any API has a Timeout of 2 seconds.
 * If in the process to load the list, the timeout is reached, the process will calculate with the rest of the lists.
 * REST API: https://my-json-server.typicode.com/jabrena/latency-problems
 */
@Slf4j
public class LatencyProblem01 {

    @Data
    @AllArgsConstructor
    public static class Config {

        private List<String> list;
        private int timeout;
    }

    private Config config;

    public LatencyProblem01(Config config) {
        this.config = config;
    }

    Function<String, URL> toURL = address -> Try.of(() ->
            new URL(address))
    .getOrElseThrow(ex -> {
        LOGGER.error(ex.getLocalizedMessage(), ex);
        throw new RuntimeException("Bad address", ex);
    });

    Function<String, Flux<String>> serializeFlux = param -> Try.of(() -> {
        if(param.length() == 0) return Flux.just("");
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> deserializedData = objectMapper.readValue(param, new TypeReference<List<String>>() {});
        return Mono.just(deserializedData).flatMapMany(Flux::fromIterable);
    }).getOrElseThrow(ex -> {
        LOGGER.error("Bad Serialization process", ex);
        throw new RuntimeException(ex);
    });

    Predicate<String> godStartingByn = s -> {
        if(s.length() == 0) return false;
        return s.toLowerCase().charAt(0) == 'n';
    };

    Function<String, List<Integer>> toDigits = s -> s.chars()
            .mapToObj(is -> Integer.valueOf(is))
            .collect(Collectors.toList());

    Function<List<Integer>, String> concatDigits = li -> li.stream()
            .map(String::valueOf)
            .collect(Collectors.joining( "" ));

    Function<Flux<String>, Mono<BigInteger>> sumFlux = ls -> ls
            .map(toDigits.andThen(concatDigits).andThen(BigInteger::new))
            .reduce(BigInteger.ZERO, (l1, l2) -> l1.add(l2));

    private Scheduler scheduler = Schedulers.elastic();

    final Flux<String> DEFAULT_FALLBACK = Flux.just("{[]}");

    Function<String, Flux<String>> asyncFetchFlux = list -> {
        return Flux.just(list)
                .publishOn(scheduler)
                .map(i -> toURL.andThen(fetch).apply(i))
                .timeout(Duration.ofSeconds(config.getTimeout()), DEFAULT_FALLBACK)
                .log()
                .flatMap(serializeFlux)
                .onErrorResume(ex -> DEFAULT_FALLBACK);
    };

    public Mono<BigInteger> reactorSolution() {

        return Flux.fromIterable(config.getList())
                .flatMap(asyncFetchFlux)
                .filter(godStartingByn)
                .transform(sumFlux)
                .doOnError(ex -> LOGGER.warn(ex.getLocalizedMessage(), ex))
                .onErrorReturn(BigInteger.ZERO)
                .next();
    }

}
