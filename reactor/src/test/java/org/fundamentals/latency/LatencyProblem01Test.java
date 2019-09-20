package org.fundamentals.latency;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.List;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

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

    private static LatencyProblem01.Config getDefaultConfig() {

        //Given
        final List<String> listOfGods = List.of(
                "http://localhost:8090/greek",
                "http://localhost:8090/roman",
                "http://localhost:8090/nordic");
        final int TIMEOUT = 2;

        //When
        return new LatencyProblem01.Config(listOfGods, TIMEOUT);
    }

    @Test
    public void given_reactorSolution_when_executeMethod_then_expectedResultsTest() {

        loadStubs();

        LatencyProblem01 problem = new LatencyProblem01(getDefaultConfig());

        StepVerifier
                .create(problem.reactorSolution())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

    @Test
    public void given_reactorSolutionFunctionalComposition_when_executeMethod_then_expectedResultsTest() {

        loadStubs();

        LatencyProblem01 problem = new LatencyProblem01(getDefaultConfig());

        StepVerifier
                .create(problem.reactorSolutionFunctionalComposition())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

    @Test
    public void given_reactorSolutionSequential_when_executeMethod_then_expectedResultsTest() {

        loadStubs();

        LatencyProblem01 problem = new LatencyProblem01(getDefaultConfig());

        StepVerifier
                .create(problem.reactorSolutionSequential())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

}
