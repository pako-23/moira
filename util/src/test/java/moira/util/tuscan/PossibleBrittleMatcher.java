package moira.util.tuscan;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.TestCase;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class PossibleBrittleMatcher extends TypeSafeMatcher<SchedulesGenerator> {
  private final Map<TestCase, Set<TestCase>> pairs;

  public PossibleBrittleMatcher(final Map<TestCase, Set<TestCase>> pairs) {
    this.pairs = pairs;
  }

  @Override
  protected boolean matchesSafely(final SchedulesGenerator generator) {
    final Set<TestCase> brittleCovered = new HashSet<>();

    while (!generator.done()) {
      final TestCase[] schedule = generator.generate();

      for (int i = 0; i < schedule.length; ++i) {
        if (coversBrittleTestCase(schedule, i)) brittleCovered.add(schedule[i]);
      }
    }

    for (final TestCase testCase : pairs.keySet())
      if (!brittleCovered.contains(testCase)) return false;

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("covers all possible brittle tests");
  }

  public static Matcher<SchedulesGenerator> coversPossibleBrittleTests(
      final Map<TestCase, Set<TestCase>> pairs) {
    return new PossibleBrittleMatcher(pairs);
  }

  private boolean coversBrittleTestCase(final TestCase[] schedule, int testIndex) {
    final Set<TestCase> targets = pairs.get(schedule[testIndex]);
    if (targets == null) return false;

    for (int j = 0; j < testIndex; ++j) if (targets.contains(schedule[j])) return false;

    return true;
  }
}
