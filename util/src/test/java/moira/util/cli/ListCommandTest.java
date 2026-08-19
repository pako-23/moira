package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ListCommandTest extends AbstractMoiraSubcommandTest {

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
    this.subcommand = "list";
    this.description = "List all the test cases within a testsuite";
  }

  @Test
  public void testEmptyTestSuite() {
    when(service.discoverTestSuite(new File("testsuite")))
        .thenReturn(new TestSuite(new ArrayList<>()));

    final int code = cmd.execute("list", "testsuite");
    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), is(emptyString()));
  }

  @Test
  public void testTestSuiteWithSingleTestCase() {
    assertListOutput(new File("testsuite"), tests[0]);
  }

  @Test
  public void testTestSuiteWithTwoTestCases() {
    assertListOutput(new File("testsuite"), tests[0], tests[1]);
  }

  @Test
  public void testTestSuiteMultipleTestCases() {
    assertListOutput(new File("testsuite"), tests);
  }

  @Test
  public void testDifferentTestsuiteFile() {
    assertListOutput(new File("someothertestsuite"), tests);
  }

  @Test
  public void testEmptyAppClassPath() {
    assertListCommand(new File("testsuite"), new String[] {"--app-cp", ""}, tests);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests"})
  public void testSetAppClassPath(final String classpath) {
    assertListCommand(new File("testsuite"), new String[] {"--app-cp", classpath}, tests);
    verify(service, times(1)).setAppClassPath(classpath);
  }

  @Test
  public void testNoTestSuite() {
    assertFailedWithMessage(cmd.execute("list"), "Missing required parameter: '<testsuite>'");
  }

  @Test
  public void testManyArguments() {
    assertFailedWithMessage(
        cmd.execute("list", "testsuite1", "testsuite2", "testsuite3"),
        "Unmatched arguments from index 2: 'testsuite2'");
  }

  @Test
  public void testHelpPageContainsParameterDescriptions() {
    final int code = cmd.execute("list", "-h");

    assertSuccessfulExecution(code);

    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<testsuite>", "The path to a file containing the testsuite")));
  }

  private void setupReturnedTests(final File file, final TestCase... testsuite) {
    when(service.discoverTestSuite(file)).thenReturn(new TestSuite(Arrays.asList(testsuite)));
  }

  private void assertTestSuiteOutput(final TestCase... testsuite) {
    final List<String> lines = Arrays.asList(stdout.toString().trim().split("\\s+"));
    final String[] expectedLines =
        Arrays.asList(testsuite).stream().map(TestCase::toString).toArray(String[]::new);
    assertThat(lines, contains(expectedLines));
  }

  private void assertListOutput(final File file, final TestCase... testsuite) {
    assertListCommand(file, new String[0], testsuite);
  }

  private void assertListCommand(
      final File file, final String[] options, final TestCase... testsuite) {
    setupReturnedTests(file, testsuite);

    final String[] args = new String[options.length + 2];
    args[0] = "list";
    System.arraycopy(options, 0, args, 1, options.length);
    args[args.length - 1] = file.toString();

    assertSuccessfulExecution(cmd.execute(args));
    assertTestSuiteOutput(testsuite);
  }
}
