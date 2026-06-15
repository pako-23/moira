package moira.util.tuscan;

import java.util.HashMap;
import java.util.Map;
import moira.util.Range;
import moira.util.TestCase;
import moira.util.TestSuite;
import moira.util.runner.ScheduleGenerator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TuscanClassOnlyMatcher extends TypeSafeMatcher<ScheduleGenerator> {
  private final TestSuite suite;

  public TuscanClassOnlyMatcher(final TestSuite suite) {
    this.suite = suite;
  }

  @Override
  protected boolean matchesSafely(final ScheduleGenerator generator) {
    final int n = suite.numberOfTestClasses();
    final boolean[][] pairs = new boolean[n][n];

    final Map<Class<?>, Integer> indices = new HashMap<>();
    for (int i = 0; i < n; ++i) {
      final Class<?> testClass = suite.getTestClass(i);
      final Range range = suite.getTestClassCases(testClass);
      indices.put(suite.getTestClass(i), i);
      if (range.max() != range.min()) continue;
      for (int j = 0; j < n; ++j) pairs[i][j] = pairs[j][i] = true;
    }

    while (!generator.done()) {
      final TestCase[] schedule = generator.generate();

      for (int i = 1; i < schedule.length; ++i) {
        final Class<?> testClass = schedule[i].getTestClass();
        final Class<?> previousTestClass = schedule[i - 1].getTestClass();

        pairs[indices.get(previousTestClass)][indices.get(testClass)] = true;
      }
    }

    for (int i = 0; i < pairs.length; ++i)
      for (int j = 0; j < pairs[i].length; ++j) if (!pairs[i][j] && i != j) return false;

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("contains all pairs between test classes");
  }

  public static Matcher<ScheduleGenerator> isTuscanClassOnlySquare(final TestSuite suite) {
    return new TuscanClassOnlyMatcher(suite);
  }
}
