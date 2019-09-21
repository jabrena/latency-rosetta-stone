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
 * Problem 1
 * Ancient European peoples worshiped many gods like Greek, Roman & Nordic gods.
 * Every God is possible to be represented as the concatenation of every character converted in Decimal.
 * Zeus = 122101117115
 *
 * Load the list of Gods and find the sum of God names starting with the letter n.
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
            new URL(address)).getOrElseThrow(ex -> {
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

    Function<Flux<String>, Flux<String>> filterGodsFlux = ls -> ls
            .filter(godStartingByn)
            .log();

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

    Function<Integer, Flux<String>> asyncFetchFlux = limit -> {
        return Flux.range(0, limit)
                .publishOn(scheduler)
                .map(i -> toURL.andThen(fetch).apply(config.getList().get(i)))
                .timeout(
                        Duration.ofSeconds(config.getTimeout()),
                        Flux.just(""))
                .flatMap(serializeFlux);
    };

    public Mono<BigInteger> reactorSolution() {

        return Flux.range(0, config.getList().size())
                .publishOn(scheduler)
                .map(i -> toURL.andThen(fetch).apply(config.getList().get(i)))
                .timeout(
                        Duration.ofSeconds(config.getTimeout()),
                        Flux.just(""))
                .flatMap(serializeFlux)
                .filter(godStartingByn)
                .log()
                .map(toDigits.andThen(concatDigits).andThen(BigInteger::new))
                .reduce(BigInteger.ZERO, (l1, l2) -> l1.add(l2));
    }

    public Mono<BigInteger> reactorSolutionSequential() {

        return Flux.range(0, config.getList().size())
                .map(i -> toURL.andThen(fetch).apply(config.getList().get(i)))
                .timeout(
                        Duration.ofSeconds(config.getTimeout()),
                        Flux.just(""))
                .flatMap(serializeFlux)
                .filter(godStartingByn)
                .log()
                .map(toDigits.andThen(concatDigits).andThen(BigInteger::new))
                .reduce(BigInteger.ZERO, (l1, l2) -> l1.add(l2));
    }

    //This implementation follow functional composition ideas from Java 8+
    //But this is not the Reactor way
    public Mono<BigInteger> reactorSolutionFunctionalComposition() {

        return asyncFetchFlux
                .andThen(filterGodsFlux)
                .andThen(sumFlux)
                .apply(config.getList().size());
    }

}
