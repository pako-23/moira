package moira.util.tuscan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleGenerator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PossibleBrittleMatcher extends TypeSafeMatcher<ScheduleGenerator> {
  private final Map<TestCase, Set<TestCase>> invertedPairs;

  public PossibleBrittleMatcher(final Map<TestCase, Set<TestCase>> pairs) {
    this.invertedPairs = new HashMap<>();

    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet()) {
      for (final TestCase testCase : entry.getValue()) {
        this.invertedPairs.computeIfAbsent(testCase, key -> new HashSet<>()).add(entry.getKey());
      }
    }
  }

  @Override
  protected boolean matchesSafely(final ScheduleGenerator generator) {
    for (int it = 0; it < generator.count(); ++it) {
      final TestCase[] schedule = generator.generate();

      for (int i = 0; i < schedule.length; ++i) {
        if (coversBrittleTestCase(schedule, i)) invertedPairs.remove(schedule[i]);
      }
    }

    return invertedPairs.isEmpty();
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("covers all possible brittle tests");
  }

  public static Matcher<ScheduleGenerator> coversPossibleBrittleTests(
      final Map<TestCase, Set<TestCase>> pairs) {
    return new PossibleBrittleMatcher(pairs);
  }

  private boolean coversBrittleTestCase(final TestCase[] schedule, int testIndex) {
    final Set<TestCase> targets = invertedPairs.get(schedule[testIndex]);
    if (targets == null) return false;

    for (int j = 0; j < testIndex; ++j) if (targets.contains(schedule[j])) return false;

    return true;
  }
}
