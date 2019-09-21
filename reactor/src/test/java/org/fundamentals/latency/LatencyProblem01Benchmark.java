package org.fundamentals.latency;

import java.math.BigInteger;
import java.util.List;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import reactor.test.StepVerifier;

public class LatencyProblem01Benchmark {

    @State(Scope.Thread)
    public static class St {

        final int TIMEOUT = 2;

        final List<String> listOfGods = List.of(
                "http://localhost:8090/greek",
                "http://localhost:8090/roman",
                "http://localhost:8090/nordic");

        LatencyProblem01 problem = new LatencyProblem01(new LatencyProblem01.Config(listOfGods, TIMEOUT));
    }

    @Benchmark
    public void ReactorSolution(St st) {

        StepVerifier
                .create(st.problem.reactorSolution())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

    @Benchmark
    public void ReactorSolutionFunctionalComposition(St st) {

        StepVerifier
                .create(st.problem.reactorSolutionFunctionalComposition())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

    @Benchmark
    public void ReactorSolutionSequential(St st) {

        StepVerifier
                .create(st.problem.reactorSolutionSequential())
                .expectNext(new BigInteger("78179288397447443426"))
                .expectComplete()
                .verify();
    }

}
