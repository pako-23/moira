package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Stream;
import moira.util.FlakyPairsCollector;
import moira.util.factory.DetectionMode;
import moira.util.runner.ScheduleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class DetectCommandTest extends AbstractMoiraSubcommandTest {
  private static final String[] emptyOutput = new String[0];
  private static final String[] singleLineOutput =
      new String[] {
        "from: com.example.Example[desc1], to: com.example.Example[desc2], type: brittle"
      };
  private static final String[] multiLineOutput =
      new String[] {
        "from: com.example.Example[desc1], to: com.example.Example[desc2], type: brittle",
        "from: com.example.Example[desc1], to: com.example.Example[desc3], type: victim",
        "from: com.example.Example[desc5], to: com.example.Example[desc4], type: victim",
        "from: com.example.Example[desc3], to: com.example.Example[desc4], type: brittle",
      };

  @Mock private ScheduleGenerator generator;
  @Mock private FlakyPairsCollector collector;
  @Captor private ArgumentCaptor<PrintWriter> outputStream;

  @BeforeEach
  public void setup() {
    super.setup();
    this.subcommand = "detect";
    this.description = "Detect dependencies between tests";
  }

  @ParameterizedTest
  @MethodSource("provideModeAndCommandOuputs")
  public void testCommandOutput(final DetectionMode mode, final String[] output) {
    final File source = new File("testsuite");

    setupDetectMocks(mode, source, output);
    assertSuccessfulExecution(cmd.execute("detect", "-m", mode.toString(), source.toString()));
    assertThat(Arrays.asList(stdout.toString().trim().split("\\n")), hasItems(output));
  }

  @Test
  public void testDefaultMode() {
    final File source = new File("testsuite");
    final DetectionMode mode = DetectionMode.TUSCAN_PACKED;
    final String[] output = multiLineOutput;

    setupDetectMocks(mode, source, output);
    assertSuccessfulExecution(cmd.execute("detect", source.toString()));
    assertThat(Arrays.asList(stdout.toString().trim().split("\\n")), hasItems(output));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests"})
  public void testSetAppClassPath(final String classpath) {
    final File source = new File("testsuite");
    final DetectionMode mode = DetectionMode.TUSCAN_PACKED;
    final String[] output = multiLineOutput;

    setupDetectMocks(mode, source, output);
    assertSuccessfulExecution(cmd.execute("detect", "--app-cp", classpath, source.toString()));
    verify(service, times(1)).setAppClassPath(classpath);
    assertThat(Arrays.asList(stdout.toString().trim().split("\\n")), hasItems(output));
  }

  @Test
  public void testSetEmptyAppClassPath() {
    final File source = new File("testsuite");
    final DetectionMode mode = DetectionMode.TUSCAN_PACKED;
    final String[] output = multiLineOutput;

    setupDetectMocks(mode, source, output);

    final int exitCode = cmd.execute("detect", "--app-cp", "", source.toString());

    assertSuccessfulExecution(exitCode);
    assertThat(Arrays.asList(stdout.toString().trim().split("\\n")), hasItems(output));
  }

  @Test
  public void testInvalidDetectionMode() {
    final int exitCode = cmd.execute("detect", "-m", "invalid", "testsuite");

    assertFailedExecution(exitCode);
    assertThat(
        stderr.toString(),
        containsString("Invalid value for option '--mode': invalid mode provided: invalid"));
  }

  @Test
  public void testMissingSource() {
    final int exitCode = cmd.execute("detect");

    assertFailedExecution(exitCode);
    assertThat(stderr.toString(), containsString("Missing required parameter: '<source>'"));
  }

  @Test
  public void testManyArguments() {
    final int exitCode = cmd.execute("detect", "source1", "source2");

    assertFailedExecution(exitCode);
    assertThat(stderr.toString(), containsString("Unmatched argument at index 2: 'source2'"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsTestsuiteParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<source>", "The file containing the testsuite or the list of test pairs")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsAppClasspathParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsModeParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "-m, --mode=<mode>",
                "Dependency detection algorithm. Valid values are: tuscan-packed, tuscan-class-only, tuscan-intra-class, tuscan-inter-class, target-pairs, moira (default: tuscan-packed)")));
  }

  private void setupDetectMocks(
      final DetectionMode mode, final File source, final String[] output) {
    when(factory.createScheduleGenerator(mode, source)).thenReturn(generator);
    when(factory.createFlakyPairsCollector(mode, source)).thenReturn(collector);
    doAnswer(
            serviceInvocation -> {
              doAnswer(
                      collectorInvocation -> {
                        for (final String line : output) outputStream.getValue().println(line);
                        return null;
                      })
                  .when(collector)
                  .print(outputStream.capture());

              return null;
            })
        .when(service)
        .findFlakyPairs(generator, collector);
  }

  private static Stream<Arguments> provideModeAndCommandOuputs() {
    return Stream.of(DetectionMode.values())
        .flatMap(
            mode ->
                Stream.of(
                    Arguments.of(mode, (Object) emptyOutput),
                    Arguments.of(mode, (Object) singleLineOutput),
                    Arguments.of(mode, (Object) multiLineOutput)));
  }
}
