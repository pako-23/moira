package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moira.model.TestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DefaultServiceProfileTest extends DefaultServiceTest {

  private static final TestCase SOURCE_ONE = TestCase.fromId("com.example.SourceTest[first]");
  private static final TestCase SOURCE_TWO = TestCase.fromId("com.example.SourceTest[second]");
  private static final TestCase TARGET_ONE = TestCase.fromId("com.example.TargetTest[first]");
  private static final TestCase TARGET_TWO = TestCase.fromId("com.example.TargetTest[second]");

  private String originalJavaVersion;

  @BeforeEach
  public void saveJavaVersion() {
    originalJavaVersion = System.getProperty("java.version");
  }

  @AfterEach
  public void restoreJavaVersion() {
    restoreProperty("java.version", originalJavaVersion);
  }

  @Test
  public void testTestSuiteFileInStandardInput() throws IOException {
    final MockedExecution[] executions = captureExecutions(1);
    final File testsuite = createTestSuiteFile("com.example.FirstTest", "com.example.SecondTest");

    service.profile(ProfileOptions.builder().withTestSuite(testsuite));

    assertThat(
        executions[0].getStdInContent(),
        is(
            String.join(System.lineSeparator(), "com.example.FirstTest", "com.example.SecondTest")
                + System.lineSeparator()));
  }

  @ParameterizedTest
  @EnumSource(Profiler.class)
  public void testProfilerArgumentsJava(final Profiler profiler) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withProfiler(profiler).withTestSuite(createTestSuiteFile()));

    final List<String> arguments = executions[0].getArguments();

    assertAgentRunnerArguments(arguments, profiler);
  }

  @ParameterizedTest
  @ValueSource(strings = {"com/example/", "com/example/,org/example/"})
  public void testFilterArgument(final String filter) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withTestSuite(createTestSuiteFile()).withFilter(filter));

    final List<String> arguments = executions[0].getArguments();

    assertAgentRunnerArguments(arguments);
    assertThat(arguments, hasItem("-Dmoira.agent.filter=" + filter));
  }

  @ParameterizedTest
  @ValueSource(strings = {"com/example/", "com/example/,org/example/"})
  public void testSuspendArgument(final String suspend) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withTestSuite(createTestSuiteFile()).withSuspend(suspend));

    final List<String> arguments = executions[0].getArguments();

    assertAgentRunnerArguments(arguments);
    assertThat(arguments, hasItem("-Dmoira.agent.suspend=" + suspend));
  }

  @Test
  public void testJava8ProfilerArgumentsOmitModuleOpens() throws IOException {
    System.setProperty("java.version", "1.8.0_382");
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(ProfileOptions.builder().withTestSuite(createTestSuiteFile()));

    final List<String> arguments = executions[0].getArguments();

    assertAgentRunnerArguments(arguments);
    assertThat(
        arguments, not(containsInRelativeOrder("--add-opens", "java.base/java.util=ALL-UNNAMED")));
    assertThat(
        arguments,
        not(containsInRelativeOrder("--add-opens", "java.base/sun.security.jca=ALL-UNNAMED")));
  }

  @Test
  public void testJava9ProfilerArgumentsIncludeModuleOpens() throws IOException {
    System.setProperty("java.version", "9.0.4");
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(ProfileOptions.builder().withTestSuite(createTestSuiteFile()));

    final List<String> arguments = executions[0].getArguments();

    assertAgentRunnerArguments(arguments);
    assertThat(
        arguments, containsInRelativeOrder("--add-opens", "java.base/java.util=ALL-UNNAMED"));
    assertThat(
        arguments,
        containsInRelativeOrder("--add-opens", "java.base/sun.security.jca=ALL-UNNAMED"));
  }

  @Test
  public void testReturnsDependencies() throws IOException {
    final MockedExecution[] executions = captureExecutions(1);
    executions[0].writeStdOutLines(
        "from: " + SOURCE_ONE + ", to: " + TARGET_ONE,
        "from: " + SOURCE_ONE + ", to: " + TARGET_TWO,
        "from: " + SOURCE_ONE + ", to: " + TARGET_ONE,
        "  from: " + SOURCE_TWO + ", to: " + TARGET_TWO + "  ");

    final Map<TestCase, Set<TestCase>> dependencies =
        service.profile(ProfileOptions.builder().withTestSuite(createTestSuiteFile()));

    assertThat(dependencies.keySet(), containsInAnyOrder(SOURCE_ONE, SOURCE_TWO));
    assertThat(dependencies.get(SOURCE_ONE), containsInAnyOrder(TARGET_ONE, TARGET_TWO));
    assertThat(dependencies.get(SOURCE_TWO), contains(TARGET_TWO));
  }

  @Test
  public void testIgnoresInvalidOutput() throws IOException {
    final MockedExecution[] executions = captureExecutions(1);
    executions[0].writeStdOutLines(
        "profiler output unrelated to dependencies",
        "from: " + SOURCE_ONE + " to: " + TARGET_ONE,
        "from: " + SOURCE_ONE + ", to: " + TARGET_ONE + ", to: " + TARGET_TWO,
        "from: invalid, to: " + TARGET_ONE,
        "from: " + SOURCE_ONE + ", to: invalid",
        "from: com.example.SourceTest[first]#01, to: " + TARGET_ONE,
        "from: " + SOURCE_ONE + ", to: com.example.TargetTest[first]#01");

    final Map<TestCase, Set<TestCase>> dependencies =
        service.profile(ProfileOptions.builder().withTestSuite(createTestSuiteFile()));

    assertThat(dependencies, anEmptyMap());
  }

  @Test
  public void testNotExistingTestSuiteFile() {
    final RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                service.profile(ProfileOptions.builder().withTestSuite(new File("not-existing"))));

    assertThat(exception.getMessage(), containsString("failed to open testsuite file"));
  }

  private static File createTestSuiteFile(final String... testClasses) throws IOException {
    final File testsuite = File.createTempFile("profile-service-", ".txt");
    testsuite.deleteOnExit();

    Files.write(testsuite.toPath(), Arrays.asList(testClasses));

    return testsuite;
  }

  private static void assertAgentRunnerArguments(
      final List<String> arguments, final Profiler profiler) {
    final String agent = Agent.path();

    assertThat(arguments.size(), greaterThan(0));
    assertThat(arguments, hasItem("-Xss2m"));
    assertThat(arguments, hasItem("-javaagent:" + agent));
    assertThat(arguments, hasItem("-Xbootclasspath/a:" + agent));
    assertThat(arguments, hasItem("-Dmoira.profiler.name=" + profiler.getProfilerClass()));
    assertThat(arguments.get(arguments.size() - 1), is(moira.service.AgentRunner.class.getName()));
  }

  private static void assertAgentRunnerArguments(final List<String> arguments) {
    assertAgentRunnerArguments(arguments, Profiler.NULL);
  }

  private static void restoreProperty(final String name, final String value) {
    if (value == null) System.clearProperty(name);
    else System.setProperty(name, value);
  }
}
