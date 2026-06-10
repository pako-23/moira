package moira.util.tuscan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.TestCase;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PairCoverMatcher extends TypeSafeMatcher<SchedulesGenerator> {
  private final Map<TestCase, Set<TestCase>> pairs;

  public PairCoverMatcher(final Map<TestCase, Set<TestCase>> pairs) {
    this.pairs = pairs;
  }

  @Override
  protected boolean matchesSafely(final SchedulesGenerator generator) {
    final Map<TestCase, Set<TestCase>> coveredPairs = new HashMap<>();

    while (!generator.done()) {
      final TestCase[] schedule = generator.generate();

      if (hasDuplicateTestCasesInSchedule(schedule)) return false;

      for (int i = 0; i < schedule.length - 1; ++i) {
        final TestCase firstTestCase = schedule[i];
        final TestCase secondTestCase = schedule[i + 1];

        coveredPairs.computeIfAbsent(firstTestCase, key -> new HashSet<>()).add(secondTestCase);
      }
    }

    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet()) {
      final Set<TestCase> covered = coveredPairs.get(entry.getKey());
      if (covered == null) return false;

      for (final TestCase testCase : entry.getValue())
        if (!covered.contains(testCase)) return false;
    }

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("covers all given test pairs");
  }

  private boolean hasDuplicateTestCasesInSchedule(final TestCase[] schedule) {
    final Set<TestCase> coveredTestCases = new HashSet<>();

    for (int i = 0; i < schedule.length; ++i) {
      if (coveredTestCases.contains(schedule[i])) return true;
      coveredTestCases.add(schedule[i]);
    }

    return false;
  }

  public static Matcher<SchedulesGenerator> coversAllPairs(
      final Map<TestCase, Set<TestCase>> pairs) {
    return new PairCoverMatcher(pairs);
  }
}
