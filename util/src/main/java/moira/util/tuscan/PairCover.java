package moira.util.tuscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleGenerator;

public final class PairCover implements ScheduleGenerator {

  private final Map<TestCase, Set<TestCase>> pairs;
  private final Map<TestCase, Set<TestCase>> invertedPairs;
  private final int count;

  public PairCover(final Map<TestCase, Set<TestCase>> pairs) {
    this.pairs = new HashMap<>();
    this.invertedPairs = new HashMap<>();
    this.count = computeCount(pairs);
    setupPairMaps(pairs);
  }

  @Override
  public TestCase[] generate() {
    if (pairs.isEmpty()) return generateToCoverInvertedPairs();

    return generateToCoverPairs();
  }

  @Override
  public int count() {
    return count;
  }

  private void setupPairMaps(final Map<TestCase, Set<TestCase>> pairs) {
    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        this.pairs.computeIfAbsent(entry.getKey(), key -> new HashSet<>()).add(testCase);

    for (final Map.Entry<TestCase, Set<TestCase>> entry : pairs.entrySet())
      for (final TestCase testCase : entry.getValue())
        this.invertedPairs.computeIfAbsent(testCase, key -> new HashSet<>()).add(entry.getKey());
  }

  private int computeCount(final Map<TestCase, Set<TestCase>> pairs) {
    int count = 0;

    setupPairMaps(pairs);
    while (!this.pairs.isEmpty() || !this.invertedPairs.isEmpty()) {
      generate();
      ++count;
    }

    return count;
  }

  private TestCase[] generateToCoverPairs() {
    final TestCase[] schedule = buildSchedule();

    removeCoveredPairs(schedule);
    removeCoveredInvertedPairs(schedule);

    return schedule;
  }

  private TestCase[] generateToCoverInvertedPairs() {
    final List<TestCase> schedule = new ArrayList<TestCase>();
    final Set<TestCase> scheduled = new HashSet<>();

    for (final Map.Entry<TestCase, Set<TestCase>> entry : invertedPairs.entrySet()) {
      if (intersects(entry.getValue(), scheduled)) continue;

      scheduled.add(entry.getKey());
      schedule.add(entry.getKey());
    }

    for (final TestCase testCase : scheduled) invertedPairs.remove(testCase);

    return schedule.stream().toArray(TestCase[]::new);
  }

  private TestCase[] buildSchedule() {
    final Set<TestCase> visited = new HashSet<>();
    final Map<TestCase, LinkedList<TestCase>> schedules = new HashMap<>();

    for (final TestCase testCase : pairs.keySet()) {
      if (visited.contains(testCase)) continue;

      final LinkedList<TestCase> segment = new LinkedList<>();
      TestCase node = testCase;

      while (node != null) {
        segment.addLast(node);
        visited.add(node);

        final Set<TestCase> targets = pairs.get(node);
        if (targets == null) break;

        node = null;
        for (final TestCase target : targets) {
          if (!visited.contains(target)) {
            node = target;
            break;
          }

          final LinkedList<TestCase> partialPath = schedules.get(target);
          if (partialPath == null) continue;

          segment.addAll(partialPath);
          schedules.remove(target);
          break;
        }
      }

      schedules.put(testCase, segment);
    }

    return schedules.values().stream().flatMap(value -> value.stream()).toArray(TestCase[]::new);
  }

  private void removeCoveredPairs(final TestCase[] schedule) {
    for (int i = 1; i < schedule.length; ++i) {
      final TestCase first = schedule[i - 1];
      final TestCase second = schedule[i];
      final Set<TestCase> targets = pairs.get(first);
      if (targets == null) continue;

      targets.remove(second);
      if (targets.isEmpty()) pairs.remove(first);
    }
  }

  private void removeCoveredInvertedPairs(final TestCase[] schedule) {
    final Set<TestCase> scheduled = new HashSet<>(schedule.length);

    for (final TestCase testCase : schedule) {
      final Set<TestCase> setters = invertedPairs.get(testCase);
      if (setters != null && !intersects(setters, scheduled)) invertedPairs.remove(testCase);

      scheduled.add(testCase);
    }
  }

  private static boolean intersects(final Set<TestCase> a, final Set<TestCase> b) {
    for (final TestCase testCase : a) if (b.contains(testCase)) return true;

    return false;
  }
}
