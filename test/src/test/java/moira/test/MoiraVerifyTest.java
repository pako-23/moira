package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.stream.Stream;
import moira.execution.Execution;
import moira.execution.ForkExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoiraVerifyTest {

  private Execution execution;
  private StringWriter stdout;

  @BeforeEach
  public void setup() {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
  }

  @Test
  public void testIndependentPair() {
    execute(
        "verify",
        testId(com.example.SimplePassingTest.class, "testPass1"),
        testId(com.example.OtherPassingTest.class, "testPass1"));

    assertThat(stdout.toString(), is("pair is independent\n"));
  }

  @Test
  public void testDependentPair() {
    execute(
        "verify",
        testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
        testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"));

    assertThat(stdout.toString(), is("pair is not independent\n"));
  }

  private static String testId(final Class<?> testClass, final String method) {
    return String.format("%s[%s(%s)]", testClass.getName(), method, testClass.getName());
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
