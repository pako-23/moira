package moira.util.tuscan;

import java.util.HashMap;
import java.util.Map;
import moira.util.model.Range;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.runner.ScheduleGenerator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TuscanIntraClassMatcher extends TypeSafeMatcher<ScheduleGenerator> {
  private final TestSuite suite;

  public TuscanIntraClassMatcher(final TestSuite suite) {
    this.suite = suite;
  }

  @Override
  protected boolean matchesSafely(final ScheduleGenerator generator) {
    final Map<String, boolean[][]> pairs = new HashMap<>(suite.numberOfTestClasses());

    for (int i = 0; i < suite.numberOfTestClasses(); ++i) {
      final String testClass = suite.getTestClass(i);
      final Range range = suite.getTestClassCases(testClass);
      final int length = range.max() - range.min();

      pairs.put(testClass, new boolean[length][length]);
    }

    final Map<TestCase, Integer> indices = new HashMap<>();
    for (int i = 0; i < suite.numberOfTestCases(); ++i) indices.put(suite.getTestCase(i), i);

    while (!generator.done()) {
      final TestCase[] schedule = generator.generate();

      for (int i = 1; i < schedule.length; ++i) {
        final TestCase test = schedule[i];
        final TestCase previousTest = schedule[i - 1];
        if (!test.getTestClass().equals(previousTest.getTestClass())) continue;
        final String testClass = test.getTestClass();
        final Range range = suite.getTestClassCases(testClass);
        final boolean[][] matrix = pairs.get(testClass);
        ;

        matrix[indices.get(previousTest) - range.min()][indices.get(test) - range.min()] = true;
      }
    }

    for (final Map.Entry<String, boolean[][]> entry : pairs.entrySet()) {
      final boolean[][] classPairs = entry.getValue();
      for (int i = 0; i < classPairs.length; ++i)
        for (int j = 0; j < classPairs[i].length; ++j)
          if (!classPairs[i][j] && i != j) return false;
    }

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("contains all pairs between test classes");
  }

  public static Matcher<ScheduleGenerator> isTuscanIntraClassSquare(final TestSuite suite) {
    return new TuscanIntraClassMatcher(suite);
  }
}
