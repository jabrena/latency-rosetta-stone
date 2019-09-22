package org.fundamentals.latency;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
public class LatencyProblem01Test {

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
                        .withBodyFile("latency-problem1/greek.json")));

        wireMockServer.stubFor(get(urlEqualTo("/roman"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem1/roman.json")));

        wireMockServer.stubFor(get(urlEqualTo("/nordic"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem1/nordic.json")));
    }

    @Disabled
    @Test
    public void given_rxJavaSolution_when_executeMethod_then_expectedResultsTest() {

        final int TIMEOUT = 2;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("MyExecutor-%d")
                .build();
        ExecutorService executor = Executors.newFixedThreadPool(
                10,
                threadFactory);

        loadStubs();

        final List<String> listOfGods = List.of(
                "http://localhost:8090/greek",
                "http://localhost:8090/roman",
                "http://localhost:8090/nordic");

        LatencyProblem01 problem = new LatencyProblem01(listOfGods, executor, TIMEOUT);

        assertThat(problem.rxJavaSolution().test()
                .assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .values().get(0)).isEqualTo(new BigInteger("78179288397447443426"));

        executor.shutdown();
    }

}
