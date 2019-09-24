package org.fundamentals.latency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

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
 *
 * @author ibaca
 */
@Slf4j
public class LatencyProblem011Test {

    private WireMockServer wireMockServer;
    private List<String> listOfGods;
    private ObjectMapper mapper;

    @BeforeEach public void setup() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        loadStubs();
        listOfGods = Arrays.asList(
                "http://localhost:8090/greek",
                "http://localhost:8090/roman",
                "http://localhost:8090/nordic");
        mapper = new ObjectMapper();
    }

    @AfterEach public void teardown() { wireMockServer.stop();}

    private void loadStubs() {//@formatter:off
        wireMockServer.stubFor(get(urlEqualTo("/greek")).willReturn(okJson("[\"Zeus\", \"Hera\", \"Poseidon\", \"Demeter\", \"Ares\", \"Athena\", \"Apollo\", \"Artemis\", \"Hephaestus\", \"Aphrodite\", \"Hermes\", \"Dionysus\", \"Hades\", \"Hypnos\", \"Nike\", \"Janus\", \"Nemesis\", \"Iris\", \"Hecate\", \"Tyche\"]")));
        wireMockServer.stubFor(get(urlEqualTo("/roman")).willReturn(okJson("[\"Venus\", \"Mars\", \"Neptun\", \"Mercury\", \"Pluto\", \"Jupiter\"]")));
        wireMockServer.stubFor(get(urlEqualTo("/nordic")).willReturn(okJson("[\"Baldur\", \"Freyja\", \"Heimdall\", \"Frigga\", \"Hel\", \"Loki\", \"Njord\", \"Odin\", \"Thor\", \"Tyr\"] ")));
    }//@formatter:on

    @Test
    public void problem1_rxjava() {
        var result = Observable.fromIterable(listOfGods)
                .flatMap(url -> Single.fromCallable(() -> fetch(url))
                        .subscribeOn(Schedulers.io()).timeout(2, SECONDS).onErrorReturnItem("[]").toObservable()
                        .flatMapIterable(res -> Arrays.asList(mapper.readValue(res, String[].class))))
                .filter(Pattern.compile("^[Nn].*").asMatchPredicate()::test)
                .map(god -> new BigInteger(god.chars().mapToObj(String::valueOf).collect(joining())))
                .reduce(BigInteger::add)
                .test();
        result.awaitTerminalEvent();
        result.assertResult(new BigInteger("78179288397447443426"));
    }

    public static String fetch(String url) throws IOException, InterruptedException {
        return HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(URI.create(url)).GET().build(), BodyHandlers.ofString())
                .body();
    }
}