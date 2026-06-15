package moira.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.runner.ScheduleRunner;

public class TuscanSquareCollector extends FlakyPairsCollector {
  private final Map<TestCase, Boolean> outcomeInIsolation;
  private final Map<TestCase, Set<TestCase>> possibleSetters;

  public TuscanSquareCollector() {
    super();
    outcomeInIsolation = new HashMap<>();
    possibleSetters = new HashMap<>();
  }

  @Override
  public void update(final ScheduleRunner.Outcome[] outcome) {
    registerOutcomeInIsolation(outcome[0]);

    for (int i = 1; i < outcome.length; ++i) {
      final ScheduleRunner.Outcome current = outcome[i];
      final ScheduleRunner.Outcome previous = outcome[i - 1];

      if (current.pass() && failsInIsolation(current.testCase()))
        registerBrittleSetter(current.testCase(), previous.testCase());
      else if (current.pass() && !outcomeInIsolation.containsKey(current.testCase()))
        possibleSetters
            .computeIfAbsent(current.testCase(), key -> new HashSet<>())
            .add(previous.testCase());
      else if (!current.pass()) registerVictimPolluter(current.testCase(), previous.testCase());
    }
  }

  private void registerOutcomeInIsolation(final ScheduleRunner.Outcome outcome) {
    outcomeInIsolation.put(outcome.testCase(), outcome.pass());

    if (!outcome.pass() && possibleSetters.containsKey(outcome.testCase())) {
      for (final TestCase setter : possibleSetters.get(outcome.testCase()))
        registerBrittleSetter(outcome.testCase(), setter);
    }

    possibleSetters.remove(outcome.testCase());
  }

  private boolean failsInIsolation(final TestCase testCase) {
    final Boolean outcome = outcomeInIsolation.get(testCase);
    return outcome != null && outcome;
  }
}
