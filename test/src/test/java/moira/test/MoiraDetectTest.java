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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MoiraDetectTest {

  private Execution execution;
  private File source;
  private StringWriter stdout;

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    source = File.createTempFile("detect-acceptance-", ".txt");
    source.deleteOnExit();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "tuscan-packed",
        "tuscan-class-only",
        "tuscan-intra-class",
        "tuscan-inter-class",
        "target-pairs",
        "moira"
      })
  public void testEmptyTestSuite(final String mode) {
    execute("detect", "--mode", mode, source.toString());

    assertThat(stdout.toString(), emptyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"tuscan-packed", "tuscan-inter-class"})
  public void testTuscanSquare(final String mode) throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("detect", "--mode", mode, source.toString());

    assertThat(
        outputLines(),
        hasItems(
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
                "victim"),
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldY"),
                "victim"),
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteFirstIndex"),
                testId(com.example.AppArrayTest.class, "testReadFirstIndex"),
                "brittle"),
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteSecondIndex"),
                testId(com.example.AppArrayTest.class, "testReadSecondIndex"),
                "brittle")));
  }

  @Test
  public void testTuscanIntraClass() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppObjectFieldTest.class.getName()));

    execute("detect", "--mode", "tuscan-intra-class", source.toString());

    assertThat(
        outputLines(),
        hasItems(
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
                "victim"),
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldY"),
                "victim")));
  }

  @Test
  public void testTuscanClassOnly() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppArrayTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppStaticFieldTest.class.getName()));

    execute("detect", "--mode", "tuscan-class-only", source.toString());

    assertThat(stdout.toString(), emptyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"target-pairs", "moira"})
  public void testPairModes(final String mode) throws IOException {
    final String arrayWriter = testId(com.example.AppArrayTest.class, "testWriteFirstIndex");
    final String arrayReader = testId(com.example.AppArrayTest.class, "testReadFirstIndex");
    final String objectWriter = testId(com.example.AppObjectFieldTest.class, "testWriteFieldX");
    final String objectReader = testId(com.example.AppObjectFieldTest.class, "testReadFieldX");

    Files.write(
        source.toPath(),
        Arrays.asList(
            pair(arrayWriter, arrayReader),
            pair(arrayReader, arrayWriter),
            pair(objectWriter, objectReader),
            pair(objectReader, objectWriter)));

    execute("detect", "--mode", mode, source.toString());

    assertThat(
        outputLines(),
        containsInAnyOrder(
            dependency(arrayWriter, arrayReader, "brittle"),
            dependency(objectWriter, objectReader, "victim")));
  }

  private List<String> outputLines() {
    return Arrays.asList(stdout.toString().trim().split("\\n"));
  }

  private static String testId(final Class<?> testClass, final String method) {
    return String.format("%s[%s(%s)]", testClass.getName(), method, testClass.getName());
  }

  private static String pair(final String from, final String to) {
    return String.format("from: %s, to: %s", from, to);
  }

  private static String dependency(final String from, final String to, final String type) {
    return String.format("from: %s, to: %s, type: %s", from, to, type);
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
