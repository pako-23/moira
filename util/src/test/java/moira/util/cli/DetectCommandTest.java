package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.PrintWriter;
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
    assertDetectOutput(mode, new File("testsuite"), output, "-m", mode.toString());
  }

  @Test
  public void testDefaultMode() {
    assertDetectOutput(DetectionMode.TUSCAN_PACKED, new File("testsuite"), multiLineOutput);
  }

  @Test
  public void testDifferentTestSuiteFile() {
    assertDetectOutput(
        DetectionMode.TUSCAN_PACKED, new File("someothertestsuite"), multiLineOutput);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests"})
  public void testSetAppClassPath(final String classpath) {
    final File source = new File("testsuite");

    setupDetectMocks(DetectionMode.TUSCAN_PACKED, source, multiLineOutput);
    assertSuccessfulExecution(cmd.execute("detect", "--app-cp", classpath, source.toString()));
    verify(service, times(1)).setAppClassPath(classpath);
    assertStdoutContainsLines(multiLineOutput);
  }

  @Test
  public void testSetEmptyAppClassPath() {
    assertDetectOutput(
        DetectionMode.TUSCAN_PACKED, new File("testsuite"), multiLineOutput, "--app-cp", "");
  }

  @Test
  public void testInvalidDetectionMode() {
    assertFailedWithMessage(
        cmd.execute("detect", "-m", "invalid", "testsuite"),
        "Invalid value for option '--mode': invalid mode provided: invalid");
  }

  @Test
  public void testMissingSource() {
    assertFailedWithMessage(cmd.execute("detect"), "Missing required parameter: '<source>'");
  }

  @Test
  public void testManyArguments() {
    assertFailedWithMessage(
        cmd.execute("detect", "source1", "source2"), "Unmatched argument at index 2: 'source2'");
  }

  @Test
  public void testHelpPageContainsParameterDescriptions() {
    final int code = cmd.execute("detect", "-h");

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "-m, --mode=<mode>",
                "Dependency detection algorithm. Valid values are: tuscan-packed, tuscan-class-only, tuscan-intra-class, tuscan-inter-class, target-pairs, moira (default: tuscan-packed)")));

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));

    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<source>", "The file containing the testsuite or the list of test pairs")));
  }

  private void assertDetectOutput(
      final DetectionMode mode, final File source, final String[] output, final String... options) {
    setupDetectMocks(mode, source, output);

    final String[] args = new String[options.length + 2];
    args[0] = "detect";
    System.arraycopy(options, 0, args, 1, options.length);
    args[args.length - 1] = source.toString();

    assertSuccessfulExecution(cmd.execute(args));
    assertStdoutContainsLines(output);
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
