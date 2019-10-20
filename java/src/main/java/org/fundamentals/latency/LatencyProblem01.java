package org.fundamentals.latency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;
import static org.fundamentals.latency.SimpleCurl.fetch;
import static org.fundamentals.latency.SimpleCurl.log;

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

    private List<String> listOfGods;
    private ExecutorService executor;
    private int TIMEOUT;

    public LatencyProblem01(List<String> listOfGods, ExecutorService executor, int timeout) {
        this.listOfGods = listOfGods;
        this.executor = executor;
        this.TIMEOUT = timeout;
    }

    Function<String, URL> toURL = address -> Try.of(() ->
            new URL(address)).getOrElseThrow(ex -> {
        LOGGER.error(ex.getLocalizedMessage(), ex);
        throw new RuntimeException("Bad address", ex);
    });

    Function<String, Stream<String>> serialize = param -> Try.of(() -> {
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> deserializedData = objectMapper.readValue(param, new TypeReference<List<String>>() {});
        return deserializedData.stream();
    }).getOrElseThrow(ex -> {
        LOGGER.error("Bad Serialization process", ex);
        throw new RuntimeException(ex);
    });

    Predicate<String> godStartingByn = s -> s.toLowerCase().charAt(0) == 'n';

    Function<String, List<Integer>> toDigits = s -> s.chars()
            .mapToObj(is -> Integer.valueOf(is))
            .collect(Collectors.toList());

    Function<List<Integer>, String> concatDigits = li -> li.stream()
            .map(String::valueOf)
            .collect(Collectors.joining( "" ));

    Consumer<String> print = LOGGER::info;

    final String DEFAULT_FETCH_ERROR = "[\"FETCH_BAD_RESULT\"]";

    Function<URL, CompletableFuture<String>> fetchAsync = address -> {

        LOGGER.info("Thread: {}", Thread.currentThread().getName());
        return CompletableFuture
                .supplyAsync(() -> fetch.andThen(log).apply(address), executor)
                .exceptionally(ex -> {
                    LOGGER.error(ex.getLocalizedMessage(), ex);
                    return DEFAULT_FETCH_ERROR;
                })
                .completeOnTimeout(DEFAULT_FETCH_ERROR, TIMEOUT, TimeUnit.SECONDS);
    };

    Function<URL, CompletableFuture<String>> fetchAsyncJ9 = address -> {

        LOGGER.info("Thread: {}", Thread.currentThread().getName());
        return CompletableFuture
                .supplyAsync(() -> fetch.andThen(log).apply(address), executor)
                .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                .handle((response, ex) -> {
                    if(!Objects.isNull(ex)) {
                        LOGGER.error(ex.getLocalizedMessage(), ex);
                        return DEFAULT_FETCH_ERROR;
                    }
                    return response;
                });
    };

    Function<URL, CompletableFuture<String>> fetchAsyncJ8 = address -> {

        LOGGER.info("Thread: {}", Thread.currentThread().getName());
        return CompletableFuture
                .supplyAsync(() ->
                    fetch.andThen(log).apply(address), executor)
                .handle((response, ex) -> {
                    if(!Objects.isNull(ex)) {
                        LOGGER.error(ex.getLocalizedMessage(), ex);
                        return DEFAULT_FETCH_ERROR;
                    }
                    return response;
                });
    };

    Function<List<String>, Stream<String>> fetchListAsync = s -> {
        List<CompletableFuture<String>> futureRequests = s.stream()
                .map(toURL.andThen(fetchAsyncJ9))
                .collect(toList());

        return futureRequests.stream()
                .map(CompletableFuture::join)
                .flatMap(serialize); //Not safe code
    };

    //Open question
    //https://stackoverflow.com/questions/58474378/is-it-possible-to-combine-more-than-2-completablefuture-in-java-8-11
    Function<List<String>, Stream<String>> fetchListAsyncCFComposition = list -> {

        URL url1 = toURL.apply(list.get(0));
        URL url2 = toURL.apply(list.get(1));
        URL url3 = toURL.apply(list.get(2));

        CompletableFuture<List<String>> completableFuture =
                CompletableFuture.supplyAsync(() -> fetch.andThen(log).apply(url1))
                .thenCombine(CompletableFuture.supplyAsync(() -> fetch.andThen(log).apply(url2)),
                //.thenCombine(CompletableFuture.supplyAsync(() -> fetch.andThen(log).apply(url3)),
                (s1, s2) -> List.of(s1,s2));
                //(s1, s2, s3) -> List.of(s1, s2, s3));

        return completableFuture.join().stream().flatMap(serialize);
    };

    Function<List<String>, Stream<String>> fetchListAsyncJ8 = s -> {
        List<CompletableFuture<String>> futureRequests = s.stream()
                .map(toURL.andThen(fetchAsyncJ8))
                .collect(toList());

        return futureRequests.stream()
                .map(cf -> {
                    try {
                        return cf.get(TIMEOUT, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        return DEFAULT_FETCH_ERROR;
                    }
                })
                .flatMap(serialize);
    };

    Function<Stream<String>, Stream<String>> filterGods = ls -> ls
            .filter(godStartingByn)
            .peek(print);

    Function<Stream<String>, BigInteger> sum = ls -> ls
            .map(toDigits.andThen(concatDigits).andThen(BigInteger::new))
            .reduce(BigInteger.ZERO, (l1, l2) -> l1.add(l2));

    public BigInteger JavaStreamSolution() {

        return fetchListAsync
                .andThen(filterGods)
                .andThen(sum)
                .apply(listOfGods);
    }

    public BigInteger Java8StreamSolution() {

        return fetchListAsyncJ8
                .andThen(filterGods)
                .andThen(sum)
                .apply(listOfGods);
    }

    public BigInteger JavaCFCompositionSolution() {

        return fetchListAsyncCFComposition
                .andThen(filterGods)
                .andThen(sum)
                .apply(listOfGods);
    }

}
