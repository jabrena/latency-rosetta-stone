package org.fundamentals.latency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.net.URL;
import java.util.Comparator;
import java.util.List;

import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.fundamentals.latency.LatencyProblem02.KEYS.GREEK;
import static org.fundamentals.latency.LatencyProblem02.KEYS.WIKIPEDIA;
import static org.fundamentals.latency.SimpleCurl.fetch;

/**
 * Problem 2
 * Greek gods are quite popular and they have presence in Wikipedia,
 * the multilingual online encyclopedia.
 * If you try to find further information about Zeus you should visit
 * the address: https://en.wikipedia.org/wiki/Zeus
 *
 * Load the list of Greek Gods and discover what is the God with more
 * literature described on Wikipedia.
 *
 * Notes:
 * Every connection with any API has a Timeout of 2 seconds.
 * If in the process to load the list, the timeout is reached, the process will calculate with the rest of the lists.
 * REST API: https://my-json-server.typicode.com/jabrena/latency-problems
 *
 */
@Slf4j
public class LatencyProblem02 {

    public static enum KEYS {
        GREEK,
        WIKIPEDIA;
    }

    @Data
    @AllArgsConstructor
    public static class Config {

        private final Map<KEYS, String> apiMap;
        private int timeout;
    }

    private Config config;

    public LatencyProblem02(Config config) {
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

    Function<String, String> generateWikiAddress = god -> this.config.apiMap.get(WIKIPEDIA) + "/" + god;

    public Mono<String> reactorSolution() {
        return Mono.just(this.config.getApiMap().get(GREEK))
                .map(toURL)
                .map(fetch)
                .map(content -> {

                    return Flux.just(content)
                            .flatMap(serializeFlux)
                            .publishOn(Schedulers.elastic())
                            .map(god -> {
                                return new Tuple2<String, Integer>(god, generateWikiAddress
                                        .andThen(toURL)
                                        .andThen(fetch)
                                        .andThen(String::length)
                                        .apply(god));
                            })
                            .publishOn(Schedulers.immediate())
                            .sort(Comparator.comparing(Tuple2::_2))
                            .takeLast(1)
                            .map(t -> t._1)
                            .log()
                            .blockLast();//TODO Review how to remove this block
                });
    }

}
