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

  public PairsCollector(final Map<TestCase, Set<TestCase>> pairs) {
    super();
    this.pairs = pairs;
    this.invertedPairs = new HashMap<>();

    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet()) {
      for (final TestCase testCase : entry.getValue()) {
        this.invertedPairs.computeIfAbsent(testCase, key -> new HashSet<>()).add(entry.getKey());
      }
    }
  }

  @Override
  public void update(final Outcome[] outcome) {
    final Set<TestCase> scheduled = new HashSet<>();

    for (int i = 1; i < outcome.length; ++i) {
      final Outcome previous = outcome[i - 1];
      final Outcome current = outcome[i];

      if (!current.pass() && isPair(previous.testCase(), current.testCase()))
        registerVictimPolluter(previous.testCase(), current.testCase());
    }

    for (int i = 0; i < outcome.length; ++i) {
      if (!outcome[i].pass() && isPossibleBrittle(outcome[i].testCase(), scheduled)) {
        for (final TestCase setter : invertedPairs.get(outcome[i].testCase()))
          registerBrittleSetter(outcome[i].testCase(), setter);
      }

      scheduled.add(outcome[i].testCase());
    }
  }

  private boolean isPair(final TestCase from, final TestCase to) {
    final Set<TestCase> targets = pairs.get(from);

    return targets != null && targets.contains(to);
  }

  private boolean isPossibleBrittle(final TestCase testCase, final Set<TestCase> scheduled) {
    return true;
  }
}
