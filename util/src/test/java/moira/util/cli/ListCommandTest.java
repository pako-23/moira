package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ListCommandTest extends AbstractMoiraSubcommandTest {

  private static final TestCase[] tests =
      new TestCase[] {
        new TestCase("com.example.TestCase", "somedescription"),
        new TestCase("com.example.TestCase", "testSomethingElse"),
        new TestCase("com.example.SomeOther", "testOther"),
        new TestCase("com.example.ExampleTest", "test1"),
        new TestCase("com.example.ExampleTest", "test2"),
        new TestCase("com.example.ExampleTest", "test3"),
        new TestCase("com.example.AppTest", "testApp"),
      };

  @BeforeEach
  public void setup() {
    super.setup();
    this.subcommand = "list";
    this.description = "List all the test cases within a testsuite";
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/app", "/app:/app/tests"})
  @NullSource
  public void testEmptyTestSuite(final String classpath) {

    when(service.discoverTestSuite(new File("testsuite"), classpathOrDefault(classpath)))
        .thenReturn(new TestSuite(new ArrayList<>()));

    final int code = listTestSuite(classpath);
    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), is(emptyString()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/app", "/app:/app/tests"})
  @NullSource
  public void testTestSuiteWithSingleTestCase(final String classpath) {
    assertListOutput(classpath, tests[0]);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/app", "/app:/app/tests"})
  @NullSource
  public void testTestSuiteWithTwoTestCases(final String classpath) {
    assertListOutput(classpath, tests[0], tests[1]);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "/app", "/app:/app/tests"})
  @NullSource
  public void testTestSuiteMultipleTestCases(final String classpath) {
    assertListOutput(classpath, tests);
  }

  @Test
  public void testNoTestSuite() {
    final int code = cmd.execute("list");

    assertFailedExecution(code);
  }

  @Test
  public void testManyArguments() {
    final int code = cmd.execute("list", "testsuite1", "testsuite2", "testsuite3");

    assertFailedExecution(code);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsTestsuiteParameter(final String help) {
    final int code = cmd.execute("list", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<testsuite>", "The path to a file containing the testsuite")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsAppClasspathParameter(final String help) {
    final int code = cmd.execute("list", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
  }

  private void assertListOutput(final String classpath, final TestCase... testsuite) {
    when(service.discoverTestSuite(new File("testsuite"), classpathOrDefault(classpath)))
        .thenReturn(new TestSuite(Arrays.asList(testsuite)));

    final int code = listTestSuite(classpath);
    assertSuccessfulExecution(code);

    final List<String> lines = Arrays.asList(stdout.toString().trim().split("\\s+"));
    final String[] expectedLines =
        Arrays.asList(testsuite).stream().map(TestCase::toString).toArray(String[]::new);
    assertThat(lines, contains(expectedLines));
  }

  private String classpathOrDefault(final String classpath) {
    return classpath == null ? "" : classpath;
  }

  private int listTestSuite(final String classpath) {
    if (classpath == null) return cmd.execute("list", "testsuite");
    else return cmd.execute("list", "--app-cp", classpath, "testsuite");
  }
}
