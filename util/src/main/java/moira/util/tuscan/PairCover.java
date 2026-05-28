package moira.util.tuscan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import moira.util.TestCase;

public final class PairCover implements SchedulesGenerator {

  private final Map<TestCase, Set<TestCase>> pairs;

  public PairCover(final Map<TestCase, Set<TestCase>> pairs) {
    this.pairs = pairs;
  }

  @Override
  public boolean done() {
    return pairs.isEmpty();
  }

  @Override
  public TestCase[] generate() {
    final TestCase[] schedule = buildSchedule();

    removeCoveredPairs(schedule);

    return schedule;
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
}
