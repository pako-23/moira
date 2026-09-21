package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import moira.model.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DefaultServiceProfileTest extends DefaultServiceTest {

  private static final TestCase SOURCE_ONE = TestCase.fromId("com.example.SourceTest[first]");
  private static final TestCase SOURCE_TWO = TestCase.fromId("com.example.SourceTest[second]");
  private static final TestCase TARGET_ONE = TestCase.fromId("com.example.TargetTest[first]");
  private static final TestCase TARGET_TWO = TestCase.fromId("com.example.TargetTest[second]");

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
  public void testProfilerArguments(final Profiler profiler) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withProfiler(profiler).withTestSuite(createTestSuiteFile()));

    final String agent = Agent.path();
    assertThat(
        executions[0].getArguments(),
        contains(
            "-javaagent:" + agent,
            "-Xbootclasspath/a:" + agent,
            "-Dmoira.profiler.name=" + profiler.getProfilerClass(),
            moira.service.AgentRunner.class.getName()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"com/example/", "com/example/,org/example/"})
  public void testFilterArgument(final String filter) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withTestSuite(createTestSuiteFile()).withFilter(filter));

    final String agent = Agent.path();
    assertThat(
        executions[0].getArguments(),
        contains(
            "-javaagent:" + agent,
            "-Xbootclasspath/a:" + agent,
            "-Dmoira.profiler.name=" + Profiler.NULL.getProfilerClass(),
            "-Dmoira.agent.filter=" + filter,
            moira.service.AgentRunner.class.getName()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"com/example/", "com/example/,org/example/"})
  public void testSuspendArgument(final String suspend) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.profile(
        ProfileOptions.builder().withTestSuite(createTestSuiteFile()).withSuspend(suspend));

    final String agent = Agent.path();
    assertThat(
        executions[0].getArguments(),
        contains(
            "-javaagent:" + agent,
            "-Xbootclasspath/a:" + agent,
            "-Dmoira.profiler.name=" + Profiler.NULL.getProfilerClass(),
            "-Dmoira.agent.suspend=" + suspend,
            moira.service.AgentRunner.class.getName()));
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
}
