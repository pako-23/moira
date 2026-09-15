package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import moira.execution.Execution;
import moira.execution.ForkExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoiraTargetPairsProfilerTest {

  private Execution execution;
  private File testsuite;
  private StringWriter stdout;

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    testsuite = File.createTempFile("target-pairs-profile-acceptance-", ".txt");
    testsuite.deleteOnExit();
  }

  @Test
  public void testMultiplePassingTests() throws IOException {
    Files.write(
        testsuite.toPath(),
        Arrays.asList(
            com.example.SimplePassingTest.class.getName(),
            com.example.OtherPassingTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", testsuite.toString());

    assertThat(stdout.toString(), emptyString());
  }

  @Test
  public void testStaticFieldDependency() throws IOException {
    Files.write(testsuite.toPath(), Arrays.asList(com.example.AppStaticFieldTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", testsuite.toString());

    final List<String> lines = outputLines();
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        containsInAnyOrder(
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testReadFieldX"),
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldX")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldX")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testReadFieldY"),
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldY")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldY"))));
  }

  @Test
  public void testObjectFieldDependency() throws IOException {
    Files.write(testsuite.toPath(), Arrays.asList(com.example.AppObjectFieldTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", testsuite.toString());

    final String[] expected =
        directedDependencies(
            com.example.AppObjectFieldTest.class,
            "testReadFieldX",
            "testReadFieldY",
            "testWriteFieldX",
            "testWriteFieldY");
    final List<String> lines = outputLines();
    assertThat(lines.size(), is(expected.length));
    assertThat(lines, containsInAnyOrder(expected));
  }

  @Test
  public void testArrayDependency() throws IOException {
    Files.write(testsuite.toPath(), Arrays.asList(com.example.AppArrayTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", testsuite.toString());

    final String[] expected =
        directedDependencies(
            com.example.AppArrayTest.class,
            "testWriteFirstIndex",
            "testWriteSecondIndex",
            "testReadFirstIndex",
            "testReadSecondIndex");
    final List<String> lines = outputLines();
    assertThat(lines.size(), is(expected.length));
    assertThat(lines, containsInAnyOrder(expected));
  }

  private List<String> outputLines() {
    return Arrays.asList(stdout.toString().trim().split("\\n"));
  }

  private static String[] directedDependencies(final Class<?> testClass, final String... methods) {
    final List<String> dependencies = new ArrayList<>();
    for (final String from : methods)
      for (final String to : methods)
        if (!from.equals(to))
          dependencies.add(dependency(testId(testClass, from), testId(testClass, to)));

    return dependencies.toArray(new String[0]);
  }

  private static String testId(final Class<?> testClass, final String method) {
    return String.format("%s[%s(%s)]", testClass.getName(), method, testClass.getName());
  }

  private static String dependency(final String from, final String to) {
    return String.format("from: %s, to: %s", from, to);
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
