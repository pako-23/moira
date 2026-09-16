package moira.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.execution.Execution;
import moira.execution.ForkExecutor;
import org.junit.jupiter.api.BeforeEach;

public abstract class AcceptanceTest {
  protected Execution execution;
  protected File source;
  protected StringWriter stdout;

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    source = File.createTempFile("moira-acceptance-test-", ".txt");
    source.deleteOnExit();
  }

  protected void execute(final String... args) {
    final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    final List<String> arguments = runtime.getInputArguments();

    execution
        .withArguments(
            Stream.concat(
                    arguments.stream().filter(name -> name.startsWith("-javaagent")),
                    Stream.concat(Stream.of(moira.cli.Moira.class.getName()), Stream.of(args)))
                .toArray(String[]::new))
        .exec();
  }

  protected List<String> outputLines() {
    return Stream.of(stdout.toString().split("\\n"))
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .collect(Collectors.toList());
  }

  protected static String testId(final Class<?> testClass, final String method) {
    return String.format("%s[%s(%s)]", testClass.getName(), method, testClass.getName());
  }

  protected static String pair(final String from, final String to) {
    return String.format("from: %s, to: %s", from, to);
  }

  protected static String dependency(final String from, final String to, final String type) {
    return String.format("from: %s, to: %s, type: %s", from, to, type);
  }

  protected static String dependency(final String from, final String to) {
    return String.format("from: %s, to: %s", from, to);
  }
}
