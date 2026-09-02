package moira.util.junit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import moira.util.model.IndexedTestCase;
import moira.util.model.Outcome;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class JUnitRunnerTest {

  private static Map<String, Outcome[]> tests;
  private JUnitRunner runner;

  static {
    tests = new HashMap<>();

    tests.put(
        com.example.JUnit4ExampleTest.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4ExampleTest.class.getName(),
                  String.format(
                      "testSimplePassing(%s)", com.example.JUnit4ExampleTest.class.getName())),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4ExampleTest.class.getName(),
                  String.format(
                      "testSimpleFailing(%s)", com.example.JUnit4ExampleTest.class.getName())),
              false),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4ExampleTest.class.getName(),
                  String.format("testIgnored(%s)", com.example.JUnit4ExampleTest.class.getName())),
              true)
        });

    tests.put(
        com.example.JUnit4SubclassTest.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4SubclassTest.class.getName(),
                  String.format("testPassing(%s)", com.example.JUnit4SubclassTest.class.getName())),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4SubclassTest.class.getName(),
                  String.format("testFailing(%s)", com.example.JUnit4SubclassTest.class.getName())),
              false),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4SubclassTest.class.getName(),
                  String.format(
                      "testAbstractMethod(%s)", com.example.JUnit4SubclassTest.class.getName())),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4SubclassTest.class.getName(),
                  String.format(
                      "testSubclassPassingTest(%s)",
                      com.example.JUnit4SubclassTest.class.getName())),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit4SubclassTest.class.getName(),
                  String.format(
                      "testSubclassFailingTest(%s)",
                      com.example.JUnit4SubclassTest.class.getName())),
              false)
        });

    tests.put(
        com.example.JUnit3ExampleTest.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit3ExampleTest.class.getName(),
                  String.format(
                      "testSimplePassing(%s)", com.example.JUnit3ExampleTest.class.getName())),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit3ExampleTest.class.getName(),
                  String.format(
                      "testSimpleFailing(%s)", com.example.JUnit3ExampleTest.class.getName())),
              false)
        });

    tests.put(
        com.example.JUnit3SuiteTestAll.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  String.format(
                      "testFailing(%s)", com.example.JUnit3FirstChildSimpleTest.class.getName())),
              false),
          new Outcome(
              new SimpleTestCase(
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  String.format(
                      "testPassing(%s)", com.example.JUnit3FirstChildSimpleTest.class.getName())),
              true),
          new Outcome(
              new IndexedTestCase(
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  String.format(
                      "testParameter(%s)", com.example.JUnit3ParametrizedTest.class.getName()),
                  0),
              true),
          new Outcome(
              new IndexedTestCase(
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  String.format(
                      "testParameter(%s)", com.example.JUnit3ParametrizedTest.class.getName()),
                  1),
              false),
          new Outcome(
              new IndexedTestCase(
                  com.example.JUnit3SuiteTestAll.class.getName(),
                  String.format(
                      "testParameter(%s)", com.example.JUnit3ParametrizedTest.class.getName()),
                  2),
              false)
        });

    tests.put(
        com.example.SingleTestCaseSuiteMethod.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.SingleTestCaseSuiteMethod.class.getName(),
                  String.format(
                      "testPassing(%s)", com.example.SingleTestCaseSuiteMethod.class.getName())),
              true)
        });

    tests.put(
        com.example.DescribableTestSuite.class.getName(),
        new Outcome[] {
          new Outcome(
              new SimpleTestCase(
                  com.example.DescribableTestSuite.class.getName(),
                  "customDescription(com.example.DescribableTestCase)"),
              true),
          new Outcome(
              new SimpleTestCase(
                  com.example.DescribableTestSuite.class.getName(),
                  "customTest(com.example.JUnit3CustomTest)"),
              true)
        });
  }

  @BeforeEach
  public void setup() {
    runner = new JUnitRunner();
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        com.example.JUnit4ExampleTest.class,
        com.example.JUnit4SubclassTest.class,
        com.example.JUnit3ExampleTest.class,
        com.example.JUnit3SuiteTestAll.class,
        com.example.SingleTestCaseSuiteMethod.class,
        com.example.DescribableTestSuite.class
      })
  public void testJUnitTestClassRun(final Class<?> testClass) {
    final List<Outcome> outcomes = runner.request(testClass.getName()).run();
    final Outcome[] expected = tests.get(testClass.getName());

    assertThat(outcomes.size(), is(expected.length));
    assertThat(outcomes, hasItems(expected));
  }

  @Test
  public void testFilterNotContainingFailing() {
    final String[] testClasses =
        new String[] {
          com.example.JUnit4ExampleTest.class.getName(),
          com.example.JUnit4SubclassTest.class.getName(),
          com.example.JUnit3ExampleTest.class.getName(),
          com.example.JUnit3SuiteTestAll.class.getName(),
          com.example.SingleTestCaseSuiteMethod.class.getName(),
          com.example.DescribableTestSuite.class.getName(),
        };

    final List<Outcome> outcomes =
        runner
            .request(testClasses)
            .withFilter(testCase -> !testCase.toString().contains("Failing"))
            .run();

    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(testClass -> Stream.of(tests.get(testClass)))
            .filter(outcome -> !outcome.testCase().toString().contains("Failing"))
            .toArray(Outcome[]::new);

    assertThat(outcomes.size(), is(expected.length));
    assertThat(outcomes, hasItems(expected));
  }

  @Test
  public void testFilterAll() {
    final String[] testClasses =
        new String[] {
          com.example.JUnit3SuiteTestAll.class.getName(),
          com.example.DescribableTestSuite.class.getName(),
        };

    final RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> runner.request(testClasses).withFilter(testCase -> false));

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
    final String[] testClasses =
        new String[] {
          com.example.JUnit4ExampleTest.class.getName(),
          com.example.JUnit4SubclassTest.class.getName(),
          com.example.JUnit3ExampleTest.class.getName(),
          com.example.JUnit3SuiteTestAll.class.getName(),
          com.example.SingleTestCaseSuiteMethod.class.getName(),
          com.example.DescribableTestSuite.class.getName(),
        };

    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(
                testClass ->
                    Stream.of(tests.get(testClass))
                        .sorted((a, b) -> comparator.compare(a.testCase(), b.testCase())))
            .toArray(Outcome[]::new);

    final List<Outcome> outcomes = runner.request(testClasses).withOrder(comparator).run();

    assertThat(outcomes, contains(expected));
  }

  @ParameterizedTest
  @MethodSource("provideComparators")
  public void testOrderingAfterFilter(final Comparator<TestCase> comparator) {
    final String[] testClasses =
        new String[] {
          com.example.JUnit4ExampleTest.class.getName(),
          com.example.JUnit4SubclassTest.class.getName(),
          com.example.JUnit3ExampleTest.class.getName(),
          com.example.JUnit3SuiteTestAll.class.getName(),
          com.example.SingleTestCaseSuiteMethod.class.getName(),
          com.example.DescribableTestSuite.class.getName(),
        };

    final Outcome[] expected =
        Stream.of(testClasses)
            .flatMap(
                testClass ->
                    Stream.of(tests.get(testClass))
                        .filter(outcome -> !outcome.testCase().toString().contains("Failing"))
                        .sorted((a, b) -> comparator.compare(a.testCase(), b.testCase())))
            .toArray(Outcome[]::new);

    final List<Outcome> outcomes =
        runner
            .request(testClasses)
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
