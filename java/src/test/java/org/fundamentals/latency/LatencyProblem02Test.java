package org.fundamentals.latency;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@Disabled
public class LatencyProblem02Test {

    @Test
    public void given_JavaStreamSolution_when_executeMethod_then_expectedResultsTest() {

        LatencyProblem02 problem = new LatencyProblem02();

        assertThat(problem.JavaStreamSolution()).isEqualTo("Apollo");
    }

    @Test
    public void given_JavaStreamSolutionAsync_when_executeMethod_then_expectedResultsTest() {

        LatencyProblem02 problem = new LatencyProblem02();

        assertThat(problem.JavaStreamSolutionAsync()).isEqualTo("Apollo");
    }

    @Test
    public void given_JavaStreamSolutionAsync2_when_executeMethod_then_expectedResultsTest() {

        LatencyProblem02 problem = new LatencyProblem02();

        assertThat(problem.JavaStreamSolutionAsync2()).isEqualTo("Apollo");
    }

}
