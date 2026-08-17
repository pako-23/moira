package moira.util.list;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.execution.Executor;
import moira.util.execution.ForkExecutor;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCasesListerTest {

  private ByteArrayOutputStream output;
  private static final Map<String, TestCase[]> testClasses;

  static {
    testClasses = new HashMap<>();

    registerTestClass(ExampleTest.class, "testExample");
    registerTestClass(SecondExampleTest.class, "testSomething", "testSomethingElse");
    registerTestClass(PrintingTest.class, "testPrinting");
  }

  @BeforeEach
  public void setup() {
    output = new ByteArrayOutputStream();
  }

  @Test
  public void testNoTestsGiven() throws IOException {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

    TestCasesLister.run(input, output);
    assertThat(output.toString(), is(emptyString()));
  }

  @Test
  public void testSingleTestClass() throws IOException {
    final String[] inputTestClasses = new String[] {ExampleTest.class.getName()};
    final ByteArrayInputStream input =
        new ByteArrayInputStream(String.join("\n", inputTestClasses).getBytes());

    TestCasesLister.run(input, output);

    assertTestCasesFound(parseDiscoveredTestCases(), inputTestClasses);
  }

  @Test
  public void testMultipleTestClass() throws IOException {
    final String[] inputTestClasses =
        new String[] {ExampleTest.class.getName(), SecondExampleTest.class.getName()};
    final ByteArrayInputStream input =
        new ByteArrayInputStream(String.join("\n", inputTestClasses).getBytes());

    TestCasesLister.run(input, output);

    assertTestCasesFound(parseDiscoveredTestCases(), inputTestClasses);
  }

  @Test
  public void testNotExistingTestClass() throws IOException {
    final String[] inputTestClasses =
        new String[] {
          ExampleTest.class.getName(),
          SecondExampleTest.class.getName(),
          "moira.util.list.SomeNotExistingClass"
        };
    final ByteArrayInputStream input =
        new ByteArrayInputStream(String.join("\n", inputTestClasses).getBytes());

    TestCasesLister.run(input, output);

    assertTestCasesFound(parseDiscoveredTestCases(), inputTestClasses);
  }

  @Test
  public void testPrintingTest() {
    final String[] inputTestClasses = new String[] {PrintingTest.class.getName()};
    final ByteArrayInputStream input =
        new ByteArrayInputStream(String.join("\n", inputTestClasses).getBytes());
    final Executor executor = new ForkExecutor();

    final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    final List<String> arguments = runtime.getInputArguments();

    final List<TestCase> discoveredTestCases = new ArrayList<>();
    executor
        .execution()
        .withArguments(
            Stream.concat(
                    arguments.stream().filter(name -> name.startsWith("-javaagent")),
                    Stream.of("moira.util.list.TestCasesLister"))
                .toArray(String[]::new))
        .withStdIn(input)
        .withStdOut(line -> discoveredTestCases.add(TestCase.fromId(line)))
        .exec();

    assertTestCasesFound(discoveredTestCases, inputTestClasses);
  }

  private void assertTestCasesFound(
      final List<TestCase> discovered, final String[] inputTestClasses) {
    final TestCase[] expected =
        Arrays.asList(inputTestClasses).stream()
            .filter(className -> testClasses.containsKey(className))
            .flatMap(className -> Arrays.asList(testClasses.get(className)).stream())
            .toArray(TestCase[]::new);

    assertThat(discovered.size(), is(expected.length));
    for (final TestCase item : expected) assertThat(discovered, hasItem(item));
  }

  private List<TestCase> parseDiscoveredTestCases() {
    return Arrays.asList(output.toString().trim().split("\\n")).stream()
        .map(TestCase::fromId)
        .collect(Collectors.toList());
  }

  private static void registerTestClass(final Class<?> clazz, final String... tests) {
    testClasses.put(
        clazz.getName(),
        Arrays.asList(tests).stream()
            .map(
                test ->
                    new SimpleTestCase(
                        clazz.getName(), String.format("%s(%s)", test, clazz.getName())))
            .toArray(TestCase[]::new));
  }
}
