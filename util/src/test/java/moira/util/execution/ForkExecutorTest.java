package moira.util.execution;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ForkExecutorTest {

  @Mock private ProcessFactory processFactory;
  @Mock private Process process;
  @Captor private ArgumentCaptor<List<String>> command;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testExecutorReturnsExecution() {
    final Executor executor = new ForkExecutor(processFactory);

    assertThat(executor.execution(), notNullValue());
  }

  @Test
  public void testSimpleExecution() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");

    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.Example")
        .exec();
    assertCommand("moira.util.execution.Example");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app:/app/tests", "~", "~/", "/app/~", "../../:../"})
  public void testSetClassPathUpdatesTheClassPath(final String classpath)
      throws IOException, InterruptedException {
    setupProcessMock(0, "", "");

    final Executor executor = new ForkExecutor(processFactory);
    executor.setClassPath(classpath);

    executor.execution().withArguments("moira.util.execution.Example").exec();

    final String expectedClassPath =
        Arrays.asList(classpath.split(":")).stream()
            .map(path -> path.replaceAll("^~", System.getProperty("user.home")))
            .map(Paths::get)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .map(Path::toString)
            .collect(Collectors.joining(":"));

    assertCommand("moira.util.execution.Example");
    assertThat(command.getValue().get(2), startsWith(expectedClassPath));
  }

  @Test
  public void testSetEmptyClassPath() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");

    final Executor executor = new ForkExecutor(processFactory);
    executor.setClassPath("");

    executor.execution().withArguments("moira.util.execution.Example").exec();

    assertCommand("moira.util.execution.Example");
    assertThat(command.getValue().get(2), is(System.getProperty("java.class.path")));
  }

  @Test
  public void testSetArgumentsMultipleTimes() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");

    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.Example")
        .withArguments("moira.util.execution.SomeOtherClass")
        .exec();
    assertCommand("moira.util.execution.SomeOtherClass");
  }

  @Test
  public void testMultipleArguemnts() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");
    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.Example", "--output", "somefile")
        .exec();

    assertCommand("moira.util.execution.Example", "--output", "somefile");
  }

  @Test
  public void testFailedProcessExecution() throws IOException, InterruptedException {
    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("moira.util.execution.Example");

    setupProcessMock(1, "", "");

    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);
    assertThat(exception.getMessage(), containsString("fork execution failed with code 1"));
    assertCommand("moira.util.execution.Example");
  }

  @Test
  public void testStdoutProcessing() throws IOException, InterruptedException {
    final StringBuffer buffer = new StringBuffer();

    setupProcessMock(0, "hello world", "");
    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.HelloWorld")
        .withStdOut(buffer::append)
        .exec();

    assertCommand("moira.util.execution.HelloWorld");
    assertThat(buffer.toString(), is("hello world"));
  }

  @Test
  public void testStderrProcessing() throws IOException, InterruptedException {
    final StringBuffer buffer = new StringBuffer();

    setupProcessMock(0, "", "hello world");
    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.HelloWorld")
        .withStdErr(buffer::append)
        .exec();

    assertCommand("moira.util.execution.HelloWorld");
    assertThat(buffer.toString(), is("hello world"));
  }

  @Test
  public void testStdoutAndStderrOutput() throws IOException, InterruptedException {
    final Random random = new Random(42);
    final String stdoutLines = generateRandomText(random);
    final String stderrLines = generateRandomText(random);
    final StringBuffer stdout = new StringBuffer();
    final StringBuffer stderr = new StringBuffer();

    setupProcessMock(0, stdoutLines, stderrLines);

    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.RandomStrings")
        .withStdErr(line -> stderr.append(line + '\n'))
        .withStdOut(line -> stdout.append(line + '\n'))
        .exec();

    assertCommand("moira.util.execution.RandomStrings");
    assertThat(stdout.toString().trim(), is(stdoutLines.trim()));
    assertThat(stderr.toString().trim(), is(stderrLines.trim()));
  }

  @Test
  public void testStdoutAndStderrOutputNoProcessing() throws IOException, InterruptedException {
    final Random random = new Random(42);
    final String stdoutLines = generateRandomText(random);
    final String stderrLines = generateRandomText(random);

    setupProcessMock(0, stdoutLines, stderrLines);
    new ForkExecutor(processFactory)
        .execution()
        .withArguments("moira.util.execution.RandomStrings")
        .exec();

    assertCommand("moira.util.execution.RandomStrings");
  }

  @Test
  public void testEchoMixedStdoutStderrOutput() {
    final Random random = new Random(42);
    final String stdoutLines = generateRandomText(random);
    final String stderrLines = generateRandomText(random);

    final StringBuffer stderr = new StringBuffer();
    final StringBuffer stdout = new StringBuffer();

    new ForkExecutor()
        .execution()
        .withArguments("moira.util.execution.MixedOutput")
        .withStdErr(line -> stderr.append(line + '\n'))
        .withStdOut(line -> stdout.append(line + '\n'))
        .withStdIn(new ByteArrayInputStream(mixLines(stdoutLines, stderrLines).getBytes()))
        .exec();

    assertThat(stdout.toString(), is(stdoutLines));
    assertThat(stderr.toString(), is(stderrLines));
  }

  @Test
  public void testNotExistingClass() {
    final Execution execution =
        new ForkExecutor().execution().withArguments("moira.util.execution.NotExistingClass");
    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);

    assertThat(exception.getMessage(), containsString("fork execution failed with code"));
  }

  @Test
  public void testExecutionNoClassGiven() {
    final Execution execution = new ForkExecutor().execution();
    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);

    assertThat(exception.getMessage(), containsString("fork execution failed with code"));
  }

  @Test
  public void testFailedProcessCreation() throws IOException {
    when(processFactory.create(command.capture())).thenThrow(IOException.class);

    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("com.example.Example");

    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);
    assertThat(exception.getMessage(), containsString("process execution failed"));
  }

  @Test
  public void testStdOutIOException() throws IOException, InterruptedException {
    when(processFactory.create(command.capture())).thenReturn(process);
    when(process.getInputStream()).thenReturn(ioexceptionInputStream());
    when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("com.example.Example");

    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);
    assertThat(exception.getMessage(), containsString("failed to read from stdout"));
  }

  @Test
  public void testStdErrIOException() throws IOException, InterruptedException {
    when(processFactory.create(command.capture())).thenReturn(process);
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(process.getErrorStream()).thenReturn(ioexceptionInputStream());

    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("com.example.Example");

    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);
    assertThat(exception.getMessage(), containsString("failed to read from stderr"));
  }

  @Test
  public void testStdInIOException() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");
    final Execution execution =
        new ForkExecutor(processFactory)
            .execution()
            .withArguments("com.example.Example")
            .withStdIn(ioexceptionInputStream());

    final RuntimeException exception = assertThrows(RuntimeException.class, execution::exec);
    assertThat(
        exception.getMessage(), containsString("failed to send input to the forked process"));
  }

  @Test
  public void testStdOutFailure() throws IOException, InterruptedException {
    when(processFactory.create(command.capture())).thenReturn(process);
    when(process.getInputStream()).thenReturn(exceptionInputStream());
    when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("com.example.Example");

    assertThrows(RuntimeException.class, execution::exec);
  }

  @Test
  public void testStdErrFailure() throws IOException, InterruptedException {
    when(processFactory.create(command.capture())).thenReturn(process);
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
    when(process.getErrorStream()).thenReturn(exceptionInputStream());

    final Execution execution =
        new ForkExecutor(processFactory).execution().withArguments("com.example.Example");

    assertThrows(RuntimeException.class, execution::exec);
  }

  @Test
  public void testStdInFailure() throws IOException, InterruptedException {
    setupProcessMock(0, "", "");
    final Execution execution =
        new ForkExecutor(processFactory)
            .execution()
            .withArguments("com.example.Example")
            .withStdIn(exceptionInputStream());

    assertThrows(RuntimeException.class, execution::exec);
  }

  private static InputStream ioexceptionInputStream() {
    return new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException();
      }
    };
  }

  private static InputStream exceptionInputStream() {
    return new InputStream() {
      @Override
      public int read() throws IOException {
        throw new AssertionError();
      }
    };
  }

  private String mixLines(final String first, final String second) {
    final String[] firstLines = first.split("\n");
    final String[] secondLines = second.split("\n");
    final StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < firstLines.length; ++i) {
      buffer.append(firstLines[i] + '\n');
      buffer.append(secondLines[i] + '\n');
    }

    return buffer.toString();
  }

  private void setupProcessMock(final int returnCode, final String stdout, final String stderr)
      throws IOException, InterruptedException {
    when(processFactory.create(command.capture())).thenReturn(process);
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(stdout.getBytes()));
    when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(stderr.getBytes()));
    when(process.waitFor()).thenReturn(returnCode);
  }

  private static String generateRandomText(final Random random) {
    final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    final StringBuffer buffer = new StringBuffer();

    for (int i = 0; i < 1300; ++i) {
      for (int j = 0; j < 100; ++j)
        buffer.append(alphabet.charAt(random.nextInt(alphabet.length())));

      buffer.append('\n');
    }

    return buffer.toString();
  }

  private void assertCommand(final String... args) {
    assertThat(command.getValue().size(), is(3 + args.length));
    assertThat(
        command.getValue().get(0),
        is(Paths.get(System.getProperty("java.home"), "bin", "java").toString()));
    assertThat(command.getValue().get(1), is("-classpath"));
    assertThat(command.getValue().get(2), containsString(System.getProperty("java.class.path")));
    for (int i = 0; i < args.length; ++i) assertThat(command.getValue().get(3 + i), is(args[i]));
  }
}
