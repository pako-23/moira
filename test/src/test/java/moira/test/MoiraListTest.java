package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.example.TestAppRegistry;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.execution.Execution;
import moira.util.execution.ForkExecutor;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoiraListTest {

  private Execution execution;
  private File testsuite;
  private StringWriter stdout;

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    testsuite = File.createTempFile("list-acceptance-", ".txt");
    testsuite.deleteOnExit();
  }

  @Test
  public void testMultipleTestClasses() throws IOException {
    final List<String> tests =
        Stream.of(
                com.example.AppArrayTest.class,
                com.example.AppObjectFieldTest.class,
                com.example.AppStaticFieldTest.class,
                com.example.OtherPassingTest.class,
                com.example.SimplePassingTest.class,
                com.example.SimpleFailingTest.class,
                com.example.JUnit4SubclassTest.class,
                com.example.JUnit3SuiteTestAll.class)
            .map(testClass -> testClass.getName())
            .collect(Collectors.toList());

    Files.write(testsuite.toPath(), tests);

    execute("list", testsuite.toString());

    final List<TestCase> listed =
        Arrays.asList(stdout.toString().trim().split("\\n")).stream()
            .map(TestCase::fromId)
            .collect(Collectors.toList());

    final TestCase[] expected =
        tests.stream()
            .flatMap(
                test ->
                    Arrays.asList(TestAppRegistry.getTestCases(test)).stream()
                        .map(TestCase::fromId))
            .toArray(TestCase[]::new);

    assertThat(listed.size(), is(expected.length));
    assertThat(listed, hasItems(expected));
  }

  @Test
  public void testEmptyTestSuite() {
    execute("list", testsuite.toString());

    assertThat(stdout.toString(), emptyString());
  }

  private void execute(final String... args) {
    final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    final List<String> arguments = runtime.getInputArguments();

    execution
        .withArguments(
            Stream.concat(
                    arguments.stream().filter(name -> name.startsWith("-javaagent")),
                    Stream.concat(Stream.of("moira.util.cli.MoiraUtil"), Stream.of(args)))
                .toArray(String[]::new))
        .exec();
  }
}
