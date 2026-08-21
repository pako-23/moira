package moira.util.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DefaultServiceIsIndependentTest extends DefaultServiceTest {

  private static final TestCase TEST_1 = TestCase.fromId("com.example.ExampleTest[desc1]");
  private static final TestCase TEST_2 = TestCase.fromId("com.example.ExampleTest[desc2]");
  private static final TestCase TEST_3 = TestCase.fromId("com.example.ExampleTest2[desc]");
  private static final TestCase TEST_4 = TestCase.fromId("com.example.ExampleTest3[desc]");

  @Test
  public void testIdependentPairs() {
    final MockedExecution[] executions = captureExecutions(2);

    executions[0].writeStdOutLines("true", "true");
    executions[1].writeStdOutLines("true", "true");

    assertThat(service.isIndependentPair(TEST_1, TEST_2), is(true));

    assertThat(
        executions[0].getArguments(), hasItem(moira.util.runner.ChildRunner.class.getName()));
    assertThat(
        executions[1].getArguments(), hasItem(moira.util.runner.ChildRunner.class.getName()));
  }

  @ParameterizedTest
  @MethodSource("provideFailureOutcomesCombinations")
  public void testFailFirstExecution(final String[] outcomes) {
    final MockedExecution[] executions = captureExecutions(1);

    executions[0].writeStdOutLines(outcomes);

    assertThat(service.isIndependentPair(TEST_1, TEST_2), is(false));
  }

  @ParameterizedTest
  @MethodSource("provideFailureOutcomesCombinations")
  public void testFailInvertedExecution(final String[] outcomes) {
    final MockedExecution[] executions = captureExecutions(2);

    executions[0].writeStdOutLines("true", "true");
    executions[1].writeStdOutLines(outcomes);

    assertThat(service.isIndependentPair(TEST_1, TEST_2), is(false));
  }

  @ParameterizedTest
  @MethodSource("produceTestPairsCombinations")
  public void testStdInContainsGivenTestPairs(final TestCase first, final TestCase second)
      throws IOException {
    final MockedExecution[] executions = captureExecutions(2);

    executions[0].writeStdOutLines("true", "true");
    executions[1].writeStdOutLines("true", "true");
    service.isIndependentPair(first, second);

    assertThat(
        Arrays.asList(executions[0].getStdInContent().trim().split("\\n")),
        contains(first.toString(), second.toString()));

    assertThat(
        Arrays.asList(executions[1].getStdInContent().trim().split("\\n")),
        contains(second.toString(), first.toString()));
  }

  @Test
  public void testInvalidLinesInExecution() {
    final MockedExecution[] executions = captureExecutions(2);

    executions[0].writeStdOutLines("true", "hello", "true");
    executions[1].writeStdOutLines("true", "true", "hello again");

    assertThat(service.isIndependentPair(TEST_1, TEST_2), is(true));
  }

  @Test
  public void testFirstExecutionMoreOutcomesThanTests() {
    final MockedExecution[] executions = captureExecutions(1);

    executions[0].writeStdOutLines("true", "true", "true");

    final RuntimeException exception =
        assertThrows(RuntimeException.class, () -> service.isIndependentPair(TEST_1, TEST_2));
    assertThat(
        exception.getMessage(), containsString("got 3 outcomes from a schedule of length 2"));
  }

  @Test
  public void testSecondExecutionMoreOutcomesThanTests() {
    final MockedExecution[] executions = captureExecutions(1);

    executions[0].writeStdOutLines("true", "true");
    executions[0].writeStdOutLines("true", "true", "false", "false");

    final RuntimeException exception =
        assertThrows(RuntimeException.class, () -> service.isIndependentPair(TEST_1, TEST_2));
    assertThat(
        exception.getMessage(), containsString("got 4 outcomes from a schedule of length 2"));
  }

  private static Stream<Arguments> provideFailureOutcomesCombinations() {
    return Stream.of(
        Arguments.of((Object) new String[] {"false", "false"}),
        Arguments.of((Object) new String[] {"false", "true"}),
        Arguments.of((Object) new String[] {"true", "false"}));
  }

  private static Stream<Arguments> produceTestPairsCombinations() {
    return Stream.of(
        Arguments.of(TEST_1, TEST_2), Arguments.of(TEST_3, TEST_4), Arguments.of(TEST_1, TEST_4));
  }
}
