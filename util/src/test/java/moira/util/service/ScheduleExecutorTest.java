package moira.util.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import com.example.JUnit4ExampleTest;
import com.example.OtherPassingTest;
import com.example.PrintingTest;
import com.example.SimplePassingTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import moira.util.execution.Executor;
import moira.util.execution.ForkExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ScheduleExecutorTest {
  private ByteArrayOutputStream output;
  private InputStream originalInput;
  private PrintStream originalOutput;
  private PrintStream originalError;

  @BeforeEach
  public void setup() {
    originalInput = System.in;
    originalOutput = System.out;
    originalError = System.err;
    output = new ByteArrayOutputStream();
  }

  @AfterEach
  public void cleanup() {
    System.setIn(originalInput);
    System.setOut(originalOutput);
    System.setErr(originalError);
  }

  @Test
  public void testExecutesScheduleInRequestedOrder() throws Exception {
    final String[] schedule = {
      testId(SimplePassingTest.class, "testPass2"),
      testId(SimplePassingTest.class, "testPass1"),
      testId(JUnit4ExampleTest.class, "testSimpleFailing"),
      testId(OtherPassingTest.class, "testPass1")
    };

    ScheduleExecutor.run(input(schedule), new PrintStream(output));

    assertThat(outputLines(), contains("true", "true", "false", "true"));
  }

  @Test
  public void testMainKeepsOutcomesOnStdoutAndRedirectsTestOutputToStderr() {
    final List<String> stdout = new ArrayList<>();
    final List<String> stderr = new ArrayList<>();
    final Executor executor = new ForkExecutor();

    executor
        .execution()
        .withArguments(
            Stream.concat(
                    ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                        .filter(argument -> argument.startsWith("-javaagent")),
                    Stream.of(ScheduleExecutor.class.getName()))
                .toArray(String[]::new))
        .withStdIn(input(testId(PrintingTest.class, "testPrinting")))
        .withStdOut(stdout::add)
        .withStdErr(stderr::add)
        .exec();

    assertThat(stdout, contains("true"));
    assertThat(stderr, hasItem("loading PrintingTest class"));
  }

  @Test
  public void testMainRequestsSuccessfulExit() throws Exception {
    final Runtime runtime = mock(Runtime.class);
    System.setIn(input(testId(SimplePassingTest.class, "testPass1")));
    System.setOut(new PrintStream(output));

    try (final MockedStatic<Runtime> runtimeStatic = mockStatic(Runtime.class)) {
      runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);
      ScheduleExecutor.main();
    }

    verify(runtime).exit(0);
    assertThat(outputLines(), contains("true"));
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
}
