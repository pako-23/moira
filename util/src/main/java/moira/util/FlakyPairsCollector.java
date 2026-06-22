package moira.util;

import java.io.PrintStream;
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

  public void print(final PrintStream stream) {
    outputPairs(stream, brittle, "brittle");
    outputPairs(stream, victims, "victim");
  }

  private void outputPairs(
      final PrintStream stream, final Map<TestCase, Set<TestCase>> pairs, final String type) {
    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        stream.printf("from: %s, to: %s, type: %s\n", entry.getKey(), testCase, type);
  }

  protected void registerBrittleSetter(final TestCase brittle, final TestCase setter) {
    registerFlakyPair(this.brittle, setter, brittle);
  }

  protected void registerVictimPolluter(final TestCase victim, final TestCase polluter) {
    registerFlakyPair(victims, polluter, victim);
  }

  private void registerFlakyPair(
      final Map<TestCase, Set<TestCase>> pairs, final TestCase from, final TestCase to) {
    pairs.computeIfAbsent(from, key -> new HashSet<>()).add(to);
  }
}
