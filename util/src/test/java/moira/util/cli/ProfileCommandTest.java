package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import moira.util.service.Profiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ProfileCommandTest extends AbstractMoiraSubcommandTest {

  private static final TestCase[] tests =
      new TestCase[] {
        TestCase.fromId("com.example.Example[desc1]"),
        TestCase.fromId("com.example.Example[desc]"),
        TestCase.fromId("com.example.Example3[desc]"),
        TestCase.fromId("com.example.Example[desc3]"),
        TestCase.fromId("com.example.Example2[desc4]"),
        TestCase.fromId("com.example.Example1[desc]"),
      };

  @BeforeEach
  public void setup() {
    super.setup();
    this.subcommand = "profile";
    this.description = "Run profiler on the given testsuite to detect flaky tests";
  }

  @ParameterizedTest
  @MethodSource("provideProfilerAndResults")
  public void testCommandOutptut(
      final Profiler profiler, final Map<TestCase, Set<TestCase>> result) {
    final File testsuite = new File("testsuite");

    when(service.profile(profiler, testsuite)).thenReturn(result);

    final int exitCode = cmd.execute("profile", "-p", profiler.toString(), testsuite.toString());

    assertSuccessfulExecution(exitCode);
    assertDetectedPairs(result);
  }

  @Test
  public void testDefaultProfiler() {
    final File testsuite = new File("testsuite");
    final Map<TestCase, Set<TestCase>> result = new HashMap<>();

    result.compute(tests[1], addDependency(tests[2]));
    result.compute(tests[3], addDependency(tests[1]));

    when(service.profile(Profiler.NULL, testsuite)).thenReturn(result);

    final int exitCode = cmd.execute("profile", testsuite.toString());

    assertSuccessfulExecution(exitCode);
    assertDetectedPairs(result);
  }

  @Test
  public void testEmptyClassPath() {
    final File testsuite = new File("testsuite");
    final Map<TestCase, Set<TestCase>> result = new HashMap<>();

    result.compute(tests[1], addDependency(tests[2]));
    result.compute(tests[3], addDependency(tests[1]));

    when(service.profile(Profiler.NULL, testsuite)).thenReturn(result);

    final int exitCode = cmd.execute("profile", "--app-cp", "", testsuite.toString());

    assertSuccessfulExecution(exitCode);
    assertDetectedPairs(result);
  }

  @Test
  public void testDifferentTestSuite() {
    final File testsuite = new File("someothertestsuite");
    final Map<TestCase, Set<TestCase>> result = new HashMap<>();

    result.compute(tests[1], addDependency(tests[2]));
    result.compute(tests[3], addDependency(tests[1]));

    when(service.profile(Profiler.NULL, testsuite)).thenReturn(result);

    final int exitCode = cmd.execute("profile", testsuite.toString());

    assertSuccessfulExecution(exitCode);
    assertDetectedPairs(result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests"})
  public void testSetAppClassPath(final String classpath) {

    final File testsuite = new File("testsuite");
    final Map<TestCase, Set<TestCase>> result = new HashMap<>();

    result.compute(tests[1], addDependency(tests[2]));
    result.compute(tests[3], addDependency(tests[1]));

    when(service.profile(Profiler.NULL, testsuite)).thenReturn(result);

    final int exitCode = cmd.execute("profile", "--app-cp", classpath, testsuite.toString());

    assertSuccessfulExecution(exitCode);
    verify(service, times(1)).setAppClassPath(classpath);
    assertDetectedPairs(result);
  }

  @Test
  public void testInvalidProfiler() {
    final int exitCode = cmd.execute("profile", "-p", "invalid", "testsuite");

    assertFailedExecution(exitCode);
    assertThat(
        stderr.toString(),
        containsString(
            "Invalid value for option '--profiler': invalid profiler provided: invalid"));
  }

  @Test
  public void testMissingSource() {
    final int exitCode = cmd.execute("profile");

    assertFailedExecution(exitCode);
    assertThat(stderr.toString(), containsString("Missing required parameter: '<testsuite>'"));
  }

  @Test
  public void testManyArguments() {
    final int exitCode = cmd.execute("profile", "testsuite1", "testsuite2");

    assertFailedExecution(exitCode);
    assertThat(stderr.toString(), containsString("Unmatched argument at index 2: 'testsuite2'"));
  }

  @Test
  public void testHelpPageContainsParameterDescriptions() {
    final int code = cmd.execute("profile", "-h");

    assertSuccessfulExecution(code);

    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<testsuite>", "The path to a file containing the testsuite")));
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "-p, --profiler=<profiler>",
                "The profiler implementation. Valid values are: online, naive, object, target-pairs, null (default: null)")));
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
  }

  private static Stream<Arguments> provideProfilerAndResults() {
    final Map<TestCase, Set<TestCase>> empty = new HashMap<>();
    final Map<TestCase, Set<TestCase>> single = new HashMap<>();
    final Map<TestCase, Set<TestCase>> multiple = new HashMap<>();

    single.compute(tests[0], addDependency(tests[1]));

    multiple.compute(tests[1], addDependency(tests[0]));
    multiple.compute(tests[1], addDependency(tests[3]));
    multiple.compute(tests[5], addDependency(tests[2]));
    multiple.compute(tests[5], addDependency(tests[1]));
    multiple.compute(tests[5], addDependency(tests[4]));

    return Arrays.asList(Profiler.values()).stream()
        .flatMap(
            profiler ->
                Stream.of(
                    Arguments.of(profiler, empty),
                    Arguments.of(profiler, single),
                    Arguments.of(profiler, multiple)));
  }

  private void assertDetectedPairs(final Map<TestCase, Set<TestCase>> result) {
    assertThat(
        Arrays.asList(stdout.toString().trim().split("\\n")),
        hasItems(
            result.entrySet().stream()
                .flatMap(
                    entry ->
                        entry.getValue().stream()
                            .map(to -> String.format("from: %s, to: %s", entry.getKey(), to)))
                .toArray(String[]::new)));
  }

  private static BiFunction<TestCase, Set<TestCase>, Set<TestCase>> addDependency(
      final TestCase test) {
    return (key, value) -> {
      if (value == null) value = new HashSet<>();
      value.add(test);
      return value;
    };
  }
}
