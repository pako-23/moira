package moira.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public class PairsCollector extends FlakyPairsCollector {
  final Map<TestCase, Set<TestCase>> pairs;
  final Map<TestCase, Set<TestCase>> invertedPairs;
  final Map<TestCase, Set<TestCase>> setters;

  public PairsCollector(final Map<TestCase, Set<TestCase>> pairs) {
    super();
    this.pairs = pairs;
    this.invertedPairs = new HashMap<>();
    this.setters = new HashMap<>();

    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        this.invertedPairs.computeIfAbsent(testCase, key -> new HashSet<>()).add(entry.getKey());
  }

  @Override
  public void update(final Outcome[] outcome) {
    final Set<TestCase> scheduled = new HashSet<>();

    for (int i = 1; i < outcome.length; ++i) {
      final Outcome previous = outcome[i - 1];
      final Outcome current = outcome[i];

      if (!isPair(previous.testCase(), current.testCase())) continue;

      if (current.pass())
        setters
            .computeIfAbsent(current.testCase(), key -> new HashSet<>())
            .add(previous.testCase());
      else registerVictimPolluter(current.testCase(), previous.testCase());
    }

    for (int i = 0; i < outcome.length; ++i) {
      final Outcome current = outcome[i];

      if (!current.pass() && isPossibleBrittle(current.testCase(), scheduled)) {
        for (final TestCase setter : setters.getOrDefault(current.testCase(), new HashSet<>()))
          registerBrittleSetter(current.testCase(), setter);
      }

      scheduled.add(current.testCase());
    }
  }

  private boolean isPair(final TestCase from, final TestCase to) {
    final Set<TestCase> targets = pairs.get(from);

    return targets != null && targets.contains(to);
  }

  private boolean isPossibleBrittle(final TestCase testCase, final Set<TestCase> scheduled) {
    final Set<TestCase> setters = invertedPairs.get(testCase);
    if (setters == null) return false;

    for (final TestCase setter : setters) if (scheduled.contains(setter)) return false;

    return true;
  }
}
