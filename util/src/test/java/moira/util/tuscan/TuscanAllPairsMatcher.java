package moira.util.tuscan;

import java.util.HashMap;
import java.util.Map;
import moira.util.TestCase;
import moira.util.TestSuite;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TuscanAllPairsMatcher extends TypeSafeMatcher<SchedulesGenerator> {
  private final TestSuite suite;

  public TuscanAllPairsMatcher(final TestSuite suite) {
    this.suite = suite;
  }

  @Override
  protected boolean matchesSafely(final SchedulesGenerator generator) {
    final int n = suite.numberOfTestCases();
    final boolean[][] pairs = new boolean[n][n];

    final Map<TestCase, Integer> indices = new HashMap<>();
    for (int i = 0; i < n; ++i) indices.put(suite.getTestCase(i), i);

    while (!generator.done()) {
      final TestCase[] schedule = generator.generate();

      for (int i = 1; i < schedule.length; ++i) {
        final TestCase test = schedule[i];
        final TestCase previousTest = schedule[i - 1];

        pairs[indices.get(previousTest)][indices.get(test)] = true;
      }
    }

    for (int i = 0; i < pairs.length; ++i)
      for (int j = 0; j < pairs[i].length; ++j) if (!pairs[i][j] && i != j) return false;

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("contains all pairs between test cases");
  }

  public static Matcher<SchedulesGenerator> isAllPairsTuscanSquare(final TestSuite suite) {
    return new TuscanClassOnlyMatcher(suite);
  }
}
