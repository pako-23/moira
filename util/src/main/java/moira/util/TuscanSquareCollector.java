package moira.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.runner.ScheduleRunner;

public class TuscanSquareCollector implements FlakyPairsCollector {
  private final Map<TestCase, Boolean> outcomeInIsolation;
  private final Map<TestCase, Set<TestCase>> possibleSetters;
  private final Map<TestCase, Set<TestCase>> brittle;
  private final Map<TestCase, Set<TestCase>> victims;

  public TuscanSquareCollector() {
    outcomeInIsolation = new HashMap<>();
    possibleSetters = new HashMap<>();
    brittle = new HashMap<>();
    victims = new HashMap<>();
  }

  @Override
  public void update(final ScheduleRunner.Outcome[] outcome) {
    registerOutcomeInIsolation(outcome[0]);

    for (int i = 1; i < outcome.length; ++i) {
      final ScheduleRunner.Outcome current = outcome[i];
      final ScheduleRunner.Outcome previous = outcome[i - 1];

      if (current.pass() && failsInIsolation(current.testCase()))
        brittle
            .computeIfAbsent(current.testCase(), key -> new HashSet<>())
            .add(previous.testCase());
      else if (current.pass() && !outcomeInIsolation.containsKey(current.testCase()))
        possibleSetters
            .computeIfAbsent(current.testCase(), key -> new HashSet<>())
            .add(previous.testCase());
      else if (!current.pass())
        victims
            .computeIfAbsent(current.testCase(), key -> new HashSet<>())
            .add(previous.testCase());
    }
  }

  @Override
  public void print() {
    outputPairs(brittle, "brittle");
    outputPairs(brittle, "victim");
  }

  private void registerOutcomeInIsolation(final ScheduleRunner.Outcome outcome) {
    outcomeInIsolation.put(outcome.testCase(), outcome.pass());

    if (!outcome.pass() && possibleSetters.containsKey(outcome.testCase()))
      brittle
          .computeIfAbsent(outcome.testCase(), key -> new HashSet<>())
          .addAll(possibleSetters.get(outcome.testCase()));

    possibleSetters.remove(outcome.testCase());
  }

  private boolean failsInIsolation(final TestCase testCase) {
    final Boolean outcome = outcomeInIsolation.get(testCase);
    return outcome != null && outcome;
  }

  private void outputPairs(final Map<TestCase, Set<TestCase>> pairs, final String type) {
    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        System.out.printf("from: %s, to: %s, type: %s\n", entry.getKey(), testCase, type);
  }
}
