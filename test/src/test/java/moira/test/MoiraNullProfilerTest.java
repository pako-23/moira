package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import moira.execution.Execution;
import moira.execution.ForkExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoiraNullProfilerTest {

  private Execution execution;
  private File testsuite;
  private StringWriter stdout;

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    testsuite = File.createTempFile("null-profile-acceptance-", ".txt");
    testsuite.deleteOnExit();
  }

  @Test
  public void testNullProfiler() throws IOException {
    Files.write(
        testsuite.toPath(),
        Arrays.asList(
            com.example.AppStaticFieldTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("profile", "--profiler", "null", testsuite.toString());

    assertThat(stdout.toString(), emptyString());
  }

  @Test
  public void testDefaultProfilerFlagIsNullProfiler() throws IOException {
    Files.write(
        testsuite.toPath(),
        Arrays.asList(
            com.example.AppStaticFieldTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("profile", testsuite.toString());

    assertThat(stdout.toString(), emptyString());
  }

  private void execute(final String... args) {
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
}
