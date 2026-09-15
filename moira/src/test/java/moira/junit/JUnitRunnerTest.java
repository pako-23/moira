package moira.junit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import com.example.TestAppRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import moira.model.Outcome;
import moira.model.SimpleTestCase;
import moira.model.TestCase;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.Runner;

public class JUnitRunnerTest {

  private JUnitRunner runner;
  private static final Class<?>[] testClasses =
      new Class<?>[] {
        com.example.JUnit4ExampleTest.class,
        com.example.JUnit4SubclassTest.class,
        com.example.JUnit3ExampleTest.class,
        com.example.JUnit3SuiteTestAll.class,
        com.example.SingleTestCaseSuiteMethod.class,
        com.example.DescribableTestSuite.class,
      };

  @BeforeEach
  public void setup() {
    runner = new JUnitRunner();
  }

  @ParameterizedTest
  @FieldSource("testClasses")
  public void testJUnitTestClassRun(final Class<?> testClass) {
    final List<Outcome> outcomes = runner.request(testClass.getName()).run();
    final Outcome[] expected =
        Stream.of(TestAppRegistry.getTestCases(testClass))
            .map(
                test -> new Outcome(TestCase.fromId(test), TestAppRegistry.isTestCasePassing(test)))
            .toArray(Outcome[]::new);

    assertThat(outcomes.size(), is(expected.length));
    assertThat(outcomes, hasItems(expected));
  }

  @Test
  public void testTestStartedCallback() {
    final Class<?> testClass = com.example.SimplePassingTest.class;
    final List<TestCase> startedTestCases = new ArrayList<>();
    final TestCase[] expected =
        Stream.of(TestAppRegistry.getTestCases(testClass))
            .map(TestCase::fromId)
            .toArray(TestCase[]::new);

    runner.request(testClass.getName()).withTestStartedCallback(startedTestCases::add).run();

    assertThat(startedTestCases.size(), is(expected.length));
    assertThat(startedTestCases, hasItems(expected));
  }

  @Test
  public void testTestFinishedCallback() {
    final Class<?> testClass = com.example.SimplePassingTest.class;
    final List<TestCase> finishedTestCases = new ArrayList<>();
    final TestCase[] expected =
        Stream.of(TestAppRegistry.getTestCases(testClass))
            .map(TestCase::fromId)
            .toArray(TestCase[]::new);

    runner.request(testClass.getName()).withTestFinishedCallback(finishedTestCases::add).run();

    assertThat(finishedTestCases.size(), is(expected.length));
    assertThat(finishedTestCases, hasItems(expected));
  }

  @Test
  public void testFilterNotContainingFailing() {
    final List<Outcome> outcomes =
        runner
            .request(Stream.of(testClasses).map(Class::getName).toArray(String[]::new))
            .withFilter(testCase -> !testCase.toString().contains("Failing"))
            .run();

    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(
                testClass ->
                    Stream.of(TestAppRegistry.getTestCases(testClass))
                        .map(
                            test ->
                                new Outcome(
                                    TestCase.fromId(test),
                                    TestAppRegistry.isTestCasePassing(test))))
            .filter(outcome -> !outcome.testCase().toString().contains("Failing"))
            .toArray(Outcome[]::new);

    assertThat(outcomes.size(), is(expected.length));
    assertThat(outcomes, hasItems(expected));
  }

  @Test
  public void testFilterAll() {
    final RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                runner
                    .request(com.example.JUnit3SuiteTestAll.class.getName())
                    .withFilter(testCase -> false));

    assertThat(
        exception.getMessage(), containsString("junit schedule run filter removed all tests"));
  }

  @Test
  public void testShouldStop() {
    final List<Outcome> outcomes =
        runner.request(com.example.JUnit3StopTestSuite.class.getName()).run();

    final Outcome expected =
        new Outcome(
            new SimpleTestCase(
                com.example.JUnit3StopTestSuite.class.getName(),
                String.format("stoppingTest(%s)", com.example.JUnit3StoppingTest.class.getName())),
            true);

    assertThat(outcomes.size(), is(1));
    assertThat(outcomes, hasItems(expected));
  }

  @ParameterizedTest
  @MethodSource("provideComparators")
  public void testOrdering(final Comparator<TestCase> comparator) {
    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(
                testClass ->
                    Stream.of(TestAppRegistry.getTestCases(testClass))
                        .map(
                            test ->
                                new Outcome(
                                    TestCase.fromId(test), TestAppRegistry.isTestCasePassing(test)))
                        .sorted((a, b) -> comparator.compare(a.testCase(), b.testCase())))
            .toArray(Outcome[]::new);

    final List<Outcome> outcomes =
        runner
            .request(Stream.of(testClasses).map(Class::getName).toArray(String[]::new))
            .withOrder(comparator)
            .run();

    assertThat(outcomes, contains(expected));
  }

  @ParameterizedTest
  @MethodSource("provideComparators")
  public void testOrderingAfterFilter(final Comparator<TestCase> comparator) {
    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(
                testClass ->
                    Stream.of(TestAppRegistry.getTestCases(testClass))
                        .map(
                            test ->
                                new Outcome(
                                    TestCase.fromId(test), TestAppRegistry.isTestCasePassing(test)))
                        .filter(outcome -> !outcome.testCase().toString().contains("Failing"))
                        .sorted((a, b) -> comparator.compare(a.testCase(), b.testCase())))
            .toArray(Outcome[]::new);

    final List<Outcome> outcomes =
        runner
            .request(Stream.of(testClasses).map(Class::getName).toArray(String[]::new))
            .withFilter(testCase -> !testCase.toString().contains("Failing"))
            .withOrder(comparator)
            .run();

    assertThat(outcomes, contains(expected));
  }

  private static Stream<Arguments> provideComparators() {
    return Stream.of(
        Arguments.of(Comparator.comparing(TestCase::toString)),
        Arguments.of(Comparator.comparing(TestCase::toString).reversed()));
  }

  @Test
  public void testNotExistingTestClass() {
    final List<Outcome> outcomes = runner.request("com.example.NotExistingTest").run();

    assertThat(outcomes.size(), is(0));
  }

  @Test
  public void testRunnerBuilderWithoutRunner() throws Throwable {
    final JUnitRunnerBuilder builder =
        new JUnitRunnerBuilder() {
          @Override
          protected JUnit4Builder junit4Builder() {
            return new JUnit4Builder() {
              @Override
              public Runner runnerForClass(final Class<?> testClass) {
                return null;
              }
            };
          }
        };

    assertThat(builder.runnerForClass(Object.class), is(nullValue()));
  }

  @ParameterizedTest
  @ValueSource(
      classes = {com.example.NonStaticSuiteMethod.class, com.example.SuiteThrowingException.class})
  public void testInitializationFailure(final Class<?> testClass) {
    final RuntimeException exception =
        assertThrows(RuntimeException.class, () -> runner.request(testClass.getName()));

    assertThat(
        exception.getMessage(), containsString("invalid test class: " + testClass.getName()));
  }
}
