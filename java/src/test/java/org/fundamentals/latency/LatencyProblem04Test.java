package org.fundamentals.latency;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.BDDAssertions.then;

@Slf4j
public class LatencyProblem04Test {

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

    @Test
    public void given_JavaStreamSolution_when_executeMethod_then_expectedResultsTest() {

        //Given
        final List<String> list = Collections.unmodifiableList(List.of(
                "KATAKROKER",
                "http://localhost:8090/transferwise",
                "http://localhost:8090/xe",
                "http://localhost:8090/iban",
                "http://localhost:8090/x-rates"));
        final ExecutorService executor = Executors.newFixedThreadPool(10,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("MyExecutor");
                    return thread;
                });
        final int timeout = 2;

        wireMockServer.stubFor(get(urlEqualTo("/transferwise"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem4/transferwise.json")));

        wireMockServer.stubFor(get(urlEqualTo("/xe"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem4/xe.json")));

        wireMockServer.stubFor(get(urlEqualTo("/iban"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem4/iban.json")));

        wireMockServer.stubFor(get(urlEqualTo("/x-rates"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBodyFile("latency-problem4/x-rates.json")));

        //When
        LatencyProblem04.Config config = new LatencyProblem04.Config(list, executor, timeout);
        LatencyProblem04 problem = new LatencyProblem04(config);

        //Then
        then(problem.JavaStreamSolution()).isEqualTo(new BigDecimal("1.1245491525"));

        executor.shutdown();
    }
}
