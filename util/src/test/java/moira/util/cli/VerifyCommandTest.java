package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class VerifyCommandTest extends AbstractMoiraSubcommandTest {

  private static final TestCase[] tests =
      new TestCase[] {
        TestCase.fromId("com.example.TestCase[somedescription]"),
        TestCase.fromId("com.example.TestCase[testSomethingElse]"),
        TestCase.fromId("com.example.SomeOther[testOther]"),
        TestCase.fromId("com.example.ExampleTest[test1]"),
        TestCase.fromId("com.example.ExampleTest[test2]"),
        TestCase.fromId("com.example.ExampleTest[test3]"),
        TestCase.fromId("com.example.AppTest[testApp]"),
      };

  @BeforeEach
  public void setup() {
    super.setup();
    this.subcommand = "verify";
    this.description = "Verifies whether a given test pair passes or not";
  }

  @ParameterizedTest
  @MethodSource("providePairsForVerify")
  public void testValidPairVerification(
      final TestCase first, final TestCase second, final boolean outcome) {
    when(service.isIndependentPair(first, second)).thenReturn(outcome);

    final int code = cmd.execute("verify", first.toString(), second.toString());
    assertSuccessfulExecution(code);

    if (outcome) assertThat(stdout.toString(), containsString("pair is independent"));
    else assertThat(stdout.toString(), containsString("pair is not independent"));
  }

  @Test
  public void testEmptyAppClassPath() {
    final TestCase first = tests[0];
    final TestCase second = tests[1];

    when(service.isIndependentPair(first, second)).thenReturn(true);

    assertSuccessfulExecution(
        cmd.execute("verify", "--app-cp", "", first.toString(), second.toString()));
    assertThat(stdout.toString(), containsString("pair is independent"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests"})
  public void testSetAppClassPath(final String classpath) {
    final TestCase first = tests[0];
    final TestCase second = tests[1];

    when(service.isIndependentPair(first, second)).thenReturn(true);

    assertSuccessfulExecution(
        cmd.execute("verify", "--app-cp", classpath, first.toString(), second.toString()));
    verify(service, times(1)).setAppClassPath(classpath);
    assertThat(stdout.toString(), containsString("pair is independent"));
  }

  @Test
  public void testInvalidTestCaseIdentifierFirstTest() {
    final int code = cmd.execute("verify", "somethingnotvalid", tests[0].toString());
    assertFailedExecution(code);

    assertThat(
        stderr.toString(),
        containsString(
            "Invalid value for positional parameter at index 0 (<first-test>): tests should have the form <class-name>[<test-description>]"));
  }

  @Test
  public void testInvalidTestCaseIdentifierSecondTest() {
    final int code = cmd.execute("verify", tests[0].toString(), "somethingnotvalid");
    assertFailedExecution(code);

    assertThat(
        stderr.toString(),
        containsString(
            "Invalid value for positional parameter at index 1 (<second-test>): tests should have the form <class-name>[<test-description>]"));
  }

  @Test
  public void testNoArguments() {
    final int code = cmd.execute("verify");
    assertFailedExecution(code);
    assertThat(
        stderr.toString(),
        containsString("Missing required parameters: '<first-test>', '<second-test>'"));
  }

  @Test
  public void testSingleTest() {
    final int code = cmd.execute("verify", tests[0].toString());
    assertFailedExecution(code);
    assertThat(stderr.toString(), containsString("Missing required parameter: '<second-test>'"));
  }

  @Test
  public void testMoreThanTwoTests() {
    final int code =
        cmd.execute(
            "verify",
            tests[0].toString(),
            tests[1].toString(),
            tests[2].toString(),
            tests[3].toString(),
            tests[4].toString());
    assertFailedExecution(code);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageDescribesTestsParmeters(final String help) {
    final int code = cmd.execute("verify", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("<first-test>", "The first test in the pair to verify")));
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("<second-test>", "The second test in the pair to verify")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsAppClasspathParameter(final String help) {
    final int code = cmd.execute("verify", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
  }

  private static Stream<Arguments> providePairsForVerify() {
    return Arrays.asList(tests).stream()
        .flatMap(
            first ->
                Arrays.asList(tests).stream()
                    .filter(second -> !second.equals(first))
                    .flatMap(
                        second ->
                            Stream.of(
                                Arguments.of(first, second, true),
                                Arguments.of(first, second, false))));
  }
}
