package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.example.TestAppRegistry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.execution.Executor;
import moira.execution.ForkExecutor;
import moira.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TestCasesListerTest {

  private ByteArrayOutputStream output;

  // private static final Map<String, TestCase[]> testClasses;

  // static {
  //   testClasses = new HashMap<>();

  //   registerTestClass(ExampleTest.class, "testExample");
  //   registerTestClass(SecondExampleTest.class, "testSomething", "testSomethingElse");
  //   registerTestClass(PrintingTest.class, "testPrinting");
  // }

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

  @ParameterizedTest
  @MethodSource("provideTestClasses")
  public void testSingleTestClass(final String[] inputTestClasses) throws IOException {
    //    final String[] inputTestClasses = new String[] {ExampleTest.class.getName()};
    final ByteArrayInputStream input =
        new ByteArrayInputStream(String.join("\n", inputTestClasses).getBytes());

    TestCasesLister.run(input, output);

    assertTestCasesFound(parseDiscoveredTestCases(), inputTestClasses);
  }

  @Test
  public void testPrintingTest() {
    final String[] inputTestClasses = new String[] {com.example.PrintingTest.class.getName()};
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
                    Stream.of(moira.service.TestCasesLister.class.getName()))
                .toArray(String[]::new))
        .withStdIn(input)
        .withStdOut(line -> discoveredTestCases.add(TestCase.fromId(line)))
        .exec();

    assertTestCasesFound(discoveredTestCases, inputTestClasses);
  }

  private static Stream<Arguments> provideTestClasses() {
    return Stream.of(
        Arguments.of((Object) new String[] {com.example.JUnit4ExampleTest.class.getName()}),
        Arguments.of((Object) new String[] {com.example.JUnit4SubclassTest.class.getName()}),
        Arguments.of((Object) new String[] {com.example.JUnit3SuiteTestAll.class.getName()}),
        Arguments.of(
            (Object)
                new String[] {
                  com.example.JUnit4ExampleTest.class.getName(),
                  com.example.JUnit4SubclassTest.class.getName(),
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  com.example.JUnit3ExampleTest.class.getName(),
                }),
        Arguments.of(
            (Object)
                new String[] {
                  com.example.JUnit4ExampleTest.class.getName(),
                  com.example.JUnit3ExampleTest.class.getName(),
                  "com.example.SomeNotExistingTestClass"
                }));
  }

  private void assertTestCasesFound(
      final List<TestCase> discovered, final String[] inputTestClasses) {
    final TestCase[] expected =
        Stream.of(inputTestClasses)
            .flatMap(
                className ->
                    Stream.of(TestAppRegistry.getTestCases(className)).map(TestCase::fromId))
            .toArray(TestCase[]::new);

    assertThat(discovered.size(), is(expected.length));
    for (final TestCase item : expected) assertThat(discovered, hasItem(item));
  }

  private List<TestCase> parseDiscoveredTestCases() {
    return Arrays.asList(output.toString().trim().split("\\n")).stream()
        .map(TestCase::fromId)
        .collect(Collectors.toList());
  }
}
