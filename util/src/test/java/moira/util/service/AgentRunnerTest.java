package moira.util.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.example.OtherPassingTest;
import com.example.PrintingTest;
import com.example.SimplePassingTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import moira.util.execution.Executor;
import moira.util.execution.ForkExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

public class AgentRunnerTest {
  private static final String PROFILER_PROPERTY = "moira.profiler.name";
  private static final String FILTER_PROPERTY = "moira.profiler.filter.filename";

  private String originalProfiler;
  private String originalFilter;
  private InputStream originalInput;
  private PrintStream originalOutput;
  private PrintStream originalError;
  private ByteArrayOutputStream output;

  @TempDir Path temporaryDirectory;

  @BeforeEach
  public void setup() {
    originalProfiler = System.getProperty(PROFILER_PROPERTY);
    originalFilter = System.getProperty(FILTER_PROPERTY);
    originalInput = System.in;
    originalOutput = System.out;
    originalError = System.err;
    output = new ByteArrayOutputStream();
  }

  @AfterEach
  public void cleanup() {
    restoreProperty(PROFILER_PROPERTY, originalProfiler);
    restoreProperty(FILTER_PROPERTY, originalFilter);
    System.setIn(originalInput);
    System.setOut(originalOutput);
    System.setErr(originalError);
  }

  @Test
  public void testAbsentProfilerAndFilterPropertiesUseDefaults() throws Exception {
    System.clearProperty(PROFILER_PROPERTY);
    System.clearProperty(FILTER_PROPERTY);

    AgentRunner.run(input(), new PrintStream(output));

    assertThat(output.toString(), is(emptyString()));
  }

  @Test
  public void testEmptyProfilerAndFilterPropertiesUseDefaults() throws Exception {
    System.setProperty(PROFILER_PROPERTY, "");
    System.setProperty(FILTER_PROPERTY, "");

    AgentRunner.run(input(), new PrintStream(output));

    assertThat(output.toString(), is(emptyString()));
  }

  @Test
  public void testExplicitProfilerRecordsEveryExecutedTest() throws Exception {
    useRecordingProfiler();

    AgentRunner.run(input(SimplePassingTest.class.getName()), new PrintStream(output));

    assertThat(
        outputLines(),
        containsInAnyOrder(
            "enter: " + testId(SimplePassingTest.class, "testPass1"),
            "exit: " + testId(SimplePassingTest.class, "testPass1"),
            "enter: " + testId(SimplePassingTest.class, "testPass2"),
            "exit: " + testId(SimplePassingTest.class, "testPass2")));
  }

  @Test
  public void testDependencyFilterRecordsOnlySourceAndTargetTests() throws Exception {
    useRecordingProfiler();
    final String source = testId(SimplePassingTest.class, "testPass1");
    final String target = testId(OtherPassingTest.class, "testPass2");
    final Path filter = temporaryDirectory.resolve("dependencies.txt");
    Files.write(
        filter, Arrays.asList("from: " + source + ", to: " + target), StandardCharsets.UTF_8);
    System.setProperty(FILTER_PROPERTY, filter.toString());

    AgentRunner.run(
        input(SimplePassingTest.class.getName(), OtherPassingTest.class.getName()),
        new PrintStream(output));

    assertThat(
        outputLines(),
        containsInAnyOrder(
            "enter: " + source, "exit: " + source, "enter: " + target, "exit: " + target));
  }

  @Test
  public void testMissingFilterWarnsAndDisablesFiltering() throws Exception {
    useRecordingProfiler();
    final Path missingFilter = temporaryDirectory.resolve("missing-filter.txt");
    final ByteArrayOutputStream error = new ByteArrayOutputStream();
    System.setErr(new PrintStream(error));
    System.setProperty(FILTER_PROPERTY, missingFilter.toString());

    AgentRunner.run(input(SimplePassingTest.class.getName()), new PrintStream(output));

    assertThat(
        error.toString(), containsString("Warning: failed to read tests filter: " + missingFilter));
    assertThat(
        outputLines(),
        containsInAnyOrder(
            "enter: " + testId(SimplePassingTest.class, "testPass1"),
            "exit: " + testId(SimplePassingTest.class, "testPass1"),
            "enter: " + testId(SimplePassingTest.class, "testPass2"),
            "exit: " + testId(SimplePassingTest.class, "testPass2")));
  }

  @Test
  public void testMainKeepsProfilerOutputOnStdoutAndRedirectsTestOutputToStderr() {
    final List<String> stdout = new ArrayList<>();
    final List<String> stderr = new ArrayList<>();
    final Executor executor = new ForkExecutor();

    executor
        .execution()
        .withArguments(
            Stream.concat(
                    ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                        .filter(argument -> argument.startsWith("-javaagent")),
                    Stream.of(
                        "-D" + PROFILER_PROPERTY + "=RecordingProfiler",
                        AgentRunner.class.getName()))
                .toArray(String[]::new))
        .withStdIn(input(PrintingTest.class.getName()))
        .withStdOut(stdout::add)
        .withStdErr(stderr::add)
        .exec();

    final String printingTest = testId(PrintingTest.class, "testPrinting");
    assertThat(stdout, contains("enter: " + printingTest, "exit: " + printingTest));
    assertThat(stderr, hasItem("loading PrintingTest class"));
  }

  @Test
  public void testMainRequestsSuccessfulExit() throws Exception {
    final Runtime runtime = mock(Runtime.class);
    System.setIn(input());
    System.setOut(new PrintStream(output));
    useRecordingProfiler();

    try (final MockedStatic<Runtime> runtimeStatic = mockStatic(Runtime.class)) {
      runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);
      AgentRunner.main();
    }

    verify(runtime).exit(0);
  }

  private void useRecordingProfiler() {
    System.setProperty(PROFILER_PROPERTY, "RecordingProfiler");
    System.clearProperty(FILTER_PROPERTY);
  }

  private static ByteArrayInputStream input(final String... lines) {
    return new ByteArrayInputStream(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
  }

  private static String testId(final Class<?> testClass, final String method) {
    return String.format("%s[%s(%s)]", testClass.getName(), method, testClass.getName());
  }

  private List<String> outputLines() {
    return Arrays.asList(output.toString().trim().split("\\R"));
  }

  private static void restoreProperty(final String name, final String value) {
    if (value == null) System.clearProperty(name);
    else System.setProperty(name, value);
  }
}
