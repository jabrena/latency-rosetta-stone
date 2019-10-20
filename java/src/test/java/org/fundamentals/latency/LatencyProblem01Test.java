package org.fundamentals.latency;

import java.math.BigInteger;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
                        .withBodyFile("greek.json")));

        wireMockServer.stubFor(get(urlEqualTo("/roman"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("roman.json")));

        wireMockServer.stubFor(get(urlEqualTo("/nordic"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("nordic.json")));
    }

    @Test
    public void given_JavaStreamSolution_when_executeMethod_then_expectedResultsTest() {

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

        assertThat(problem.JavaStreamSolution()).isEqualTo(new BigInteger("78179288397447443426"));

        executor.shutdown();
    }

    @Test
    public void given_Java8StreamSolution_when_executeMethod_then_expectedResultsTest() {

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

        assertThat(problem.Java8StreamSolution()).isEqualTo(new BigInteger("78179288397447443426"));

        executor.shutdown();
    }

    @Disabled
    @Tag("endtoend")
    @Test
    public void given_JavaStreamSolution_when_executeMethod_then_expectedResultsEndToEndTest() {

        final int TIMEOUT = 2;

        ExecutorService executor = Executors.newFixedThreadPool(10,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("MyExecutor");
                    return thread;
                });

        final List<String> listOfGodsOriginal = List.of(
                "http://my-json-server.typicode.com/jabrena/latency-problems/greek",
                "http://my-json-server.typicode.com/jabrena/latency-problems/roman",
                "http://my-json-server.typicode.com/jabrena/latency-problems/nordic");

        LatencyProblem01 problem = new LatencyProblem01(listOfGodsOriginal, executor, TIMEOUT);

        assertThat(problem.JavaStreamSolution()).isEqualTo(new BigInteger("78179288397447443426"));

        executor.shutdown();
    }

}
