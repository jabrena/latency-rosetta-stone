package org.fundamentals.latency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static org.fundamentals.latency.SimpleCurl.*;

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

    Predicate<String> godStartingByn = s -> s.toLowerCase().charAt(0) == 'n';

    Function<String, List<Integer>> toDigits = s -> s.chars()
            .mapToObj(is -> Integer.valueOf(is))
            .collect(Collectors.toList());

    Function<List<Integer>, String> concatDigits = li -> li.stream()
            .map(String::valueOf)
            .collect(Collectors.joining( "" ));

    Consumer<String> print = LOGGER::info;
    Consumer<BigInteger> print2 = bi -> LOGGER.info("{}", bi);

    Function<List<String>, Observable<List<BigInteger>>> rxFetchListAsync = s -> {

        Observable<List<BigInteger>> call1 = Observable.just(s.get(0))
                //.subscribeOn(Schedulers.io())
                .map(str -> toURL.apply(str))
                .map(u -> fetch.apply(u))
                .map(str -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<String> deserializedData = objectMapper.readValue(str, new TypeReference<List<String>>() {});
                    return deserializedData.stream()
                            .filter(godStartingByn)
                            .peek(print)
                            .map(ss -> toDigits.andThen(concatDigits).andThen(BigInteger::new).apply(ss))
                            .collect(Collectors.toUnmodifiableList());
                });

        var result = Observable
                //.subscribeOn(Schedulers.io())
                .concat(call1, call1);

         return result;
    };

    Function<List<String>, BigInteger> sum = ls -> ls.stream()
            .map(toDigits.andThen(concatDigits).andThen(BigInteger::new))
            .reduce(BigInteger.ZERO, (l1, l2) -> l1.add(l2));

    public Observable<List<BigInteger>> rxJavaSolution() {

        return rxFetchListAsync.apply(listOfGods);
    }
}
