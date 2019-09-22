package org.fundamentals.latency;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.fundamentals.latency.LatencyProblem02.KEYS.GREEK;
import static org.fundamentals.latency.LatencyProblem02.KEYS.WIKIPEDIA;

@Slf4j
public class LatencyProblem02Test {

    WireMockServer wireMockServer;

    @BeforeEach
    public void setup () {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
    }

    @AfterEach
    public void teardown () {
        wireMockServer.stop();
    }

    private void loadStubs() {

        wireMockServer.stubFor(get(urlEqualTo("/greek"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("greek.json")));

        wireMockServer.stubFor(get(urlPathEqualTo("/wiki/Apollo"))
                .willReturn(aResponse().withHeader("Content-Type", "text/html; charset=utf-8")
                        .withStatus(200)
                        .withBodyFile("apollo.html")));

        wireMockServer.stubFor(get(urlMatching("/wiki/(Zeus|Hera|Poseidon|Demeter|Ares|Athena|Artemis|Hephaestus|Aphrodite|Hermes|Dionysus|Hades|Hypnos|Nike|Janus|Nemesis|Iris|Hecate|Tyche)"))
                .willReturn(aResponse().withHeader("Content-Type", "text/html; charset=utf-8")
                        .withStatus(200)
                        .withBodyFile("zeus.html")));

    }

    private static LatencyProblem02.Config getDefaultConfig() {

        //Given
        final Map<LatencyProblem02.KEYS, String> apiMap = Map.of(
                GREEK, "http://localhost:8090/greek",
                WIKIPEDIA, "http://localhost:8090/wiki");
        final int TIMEOUT = 2;

        //When
        return new LatencyProblem02.Config(apiMap, TIMEOUT);
    }

    @Test
    public void given_reactorSolution_when_executeMethod_then_expectedResultsTest() {

        loadStubs();

        LatencyProblem02 problem = new LatencyProblem02(getDefaultConfig());

        StepVerifier
                .create(problem.reactorSolution())
                .expectNext("Apollo")
                .expectComplete()
                .verify();
    }

}
