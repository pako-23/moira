package moira.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public abstract class FlakyPairsCollector {
  private final Map<TestCase, Set<TestCase>> brittle;
  private final Map<TestCase, Set<TestCase>> victims;

  public FlakyPairsCollector() {
    brittle = new HashMap<>();
    victims = new HashMap<>();
  }

  public abstract void update(final Outcome[] outcome);

  public void print() {
    outputPairs(brittle, "brittle");
    outputPairs(victims, "victim");
  }

  private void outputPairs(final Map<TestCase, Set<TestCase>> pairs, final String type) {
    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        System.out.printf("from: %s, to: %s, type: %s\n", entry.getKey(), testCase, type);
  }

  protected void registerBrittleSetter(final TestCase brittle, final TestCase setter) {
    registerFlakyPair(this.brittle, brittle, setter);
  }

  protected void registerVictimPolluter(final TestCase victim, final TestCase polluter) {
    registerFlakyPair(victims, victim, polluter);
  }

  private void registerFlakyPair(
      final Map<TestCase, Set<TestCase>> pairs, final TestCase first, final TestCase second) {
    pairs.computeIfAbsent(first, key -> new HashSet<>()).add(second);
  }
}
