package moira.util.tuscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleGenerator;

public final class TargetPairsGenerator implements ScheduleGenerator {
  private final Map<TestCase, Set<TestCase>> crossClassPairs;
  private final Map<TestCase, Set<TestCase>> intraClassPairs;
  private final int count;

  public TargetPairsGenerator(final Map<TestCase, Set<TestCase>> pairs) {

    crossClassPairs = new HashMap<>();
    intraClassPairs = new HashMap<>();
    count = computeCount(pairs);

    populateMapPairs(pairs);
  }

  @Override
  public TestCase[] generate() {
    final TestCase[] schedule = buildSchedule().stream().toArray(TestCase[]::new);

    removeCoveredPairs(schedule);

    return schedule;
  }

  @Override
  public int count() {
    return count;
  }

  private int computeCount(final Map<TestCase, Set<TestCase>> pairs) {
    int count = 0;

    populateMapPairs(pairs);
    while (!crossClassPairs.isEmpty() || !intraClassPairs.isEmpty()) {
      generate();
      ++count;
    }

    return count;
  }

  private void populateMapPairs(final Map<TestCase, Set<TestCase>> pairs) {

    for (final Map.Entry<TestCase, Set<TestCase>> pair : pairs.entrySet()) {
      final TestCase dependant = pair.getKey();

      for (final TestCase dependee : pair.getValue()) {
        Map<TestCase, Set<TestCase>> destinationMap = crossClassPairs;

        if (dependee.getTestClass().equals(dependant.getTestClass())) {
          destinationMap = intraClassPairs;
        }

        destinationMap.computeIfAbsent(dependant, key -> new HashSet<>()).add(dependee);
      }
    }
  }

  private List<TestCase> buildSchedule() {
    if (crossClassPairs.isEmpty()) return buildIntraClassSchedule();
    return buildCrossClassSchedule();
  }

  private List<TestCase> buildIntraClassSchedule() {
    final List<TestCase> schedule = new ArrayList<>();
    final Set<String> scheduledClasses = new HashSet<>();

    for (final TestCase testCase : intraClassPairs.keySet()) {
      if (scheduledClasses.contains(testCase.getTestClass())) continue;
      schedule.add(testCase);
      schedule.addAll(findPath(testCase));
      scheduledClasses.add(testCase.getTestClass());
    }

    return schedule;
  }

  private List<TestCase> buildCrossClassSchedule() {
    final Set<String> scheduledClasses = new HashSet<>();
    final LinkedList<TestCase> schedule = new LinkedList<>();

    final TestCase mostFrequentTestCase = findMostFrequentTestCase();

    scheduledClasses.add(mostFrequentTestCase.getTestClass());
    schedule.add(mostFrequentTestCase);

    if (countWithTestCaseOnLeft(mostFrequentTestCase)
        > countWithTestCaseOnRight(mostFrequentTestCase)) {
      final TestCase testCaseOnRight =
          findMostFrequentWithTestCaseOnLeft(mostFrequentTestCase, scheduledClasses).get();
      schedule.add(testCaseOnRight);
      scheduledClasses.add(testCaseOnRight.getTestClass());
    } else {
      final TestCase testCaseOnLeft =
          findMostFrequentWithTestCaseOnRight(mostFrequentTestCase, scheduledClasses).get();
      schedule.addFirst(testCaseOnLeft);
      scheduledClasses.add(testCaseOnLeft.getTestClass());
    }

    Optional<TestCase> testCaseOnLeft = Optional.of(schedule.getFirst());
    Optional<TestCase> testCaseOnRight = Optional.of(schedule.getLast());

    while (testCaseOnLeft.isPresent() || testCaseOnRight.isPresent()) {
      if (testCaseOnLeft.isPresent()) {
        testCaseOnLeft =
            findMostFrequentWithTestCaseOnRight(testCaseOnLeft.get(), scheduledClasses);
        if (testCaseOnLeft.isPresent()) {
          schedule.addFirst(testCaseOnLeft.get());
          scheduledClasses.add(testCaseOnLeft.get().getTestClass());
        }
      }

      if (testCaseOnRight.isPresent()) {
        testCaseOnRight =
            findMostFrequentWithTestCaseOnLeft(testCaseOnRight.get(), scheduledClasses);
        if (testCaseOnRight.isPresent()) {
          schedule.addLast(testCaseOnRight.get());
          scheduledClasses.add(testCaseOnRight.get().getTestClass());
        }
      }
    }

    fillIntraClassPairs(schedule);

    return schedule;
  }

  private void removeCoveredPairs(final TestCase[] schedule) {
    for (int i = 1; i < schedule.length; ++i) {
      final TestCase first = schedule[i - 1];
      final TestCase second = schedule[i];

      if (first.getTestClass().equals(second.getTestClass()))
        removePair(intraClassPairs, first, second);
      else removePair(crossClassPairs, first, second);
    }
  }

  private void removePair(
      final Map<TestCase, Set<TestCase>> pairs, final TestCase first, final TestCase second) {
    final Set<TestCase> targets = pairs.get(first);
    if (targets == null) return;

    targets.remove(second);
    if (targets.isEmpty()) pairs.remove(first);
  }

  private Optional<TestCase> findMostFrequentWithTestCaseOnLeft(
      final TestCase testCase, final Set<String> filter) {

    final Set<TestCase> candidates = crossClassPairs.get(testCase);
    if (candidates == null) return Optional.empty();

    return findMostFrequentTestCaseFromCandidates(
        candidates.stream()
            .filter(item -> !filter.contains(item.getTestClass()))
            .collect(Collectors.toSet()));
  }

  private long countWithTestCaseOnLeft(final TestCase testCase) {
    return crossClassPairs.getOrDefault(testCase, new HashSet<>()).size();
  }

  private long countWithTestCaseOnRight(final TestCase testCase) {
    return crossClassPairs.entrySet().stream()
        .flatMap(entry -> entry.getValue().stream())
        .filter(item -> item.equals(testCase))
        .count();
  }

  private Optional<TestCase> findMostFrequentWithTestCaseOnRight(
      final TestCase testCase, final Set<String> filter) {
    final Set<TestCase> candidates =
        crossClassPairs.entrySet().stream()
            .filter(
                entry ->
                    !filter.contains(entry.getKey().getTestClass())
                        && entry.getValue().contains(testCase))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

    return findMostFrequentTestCaseFromCandidates(candidates);
  }

  private Optional<TestCase> findMostFrequentTestCaseFromCandidates(
      final Set<TestCase> candidates) {
    return crossClassPairs.entrySet().stream()
        .flatMap(
            entry -> {
              final Stream<TestCase> stream =
                  entry.getValue().stream().filter(item -> candidates.contains(item));
              if (candidates.contains(entry.getKey()))
                return Stream.concat(Stream.of(entry.getKey()), stream);
              return stream;
            })
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
  }

  private TestCase findMostFrequentTestCase() {
    return crossClassPairs.entrySet().stream()
        .flatMap(entry -> Stream.concat(Stream.of(entry.getKey()), entry.getValue().stream()))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .get();
  }

  private void fillIntraClassPairs(final LinkedList<TestCase> schedule) {
    final ListIterator<TestCase> it = schedule.listIterator();

    while (it.hasNext()) {
      final TestCase testCase = it.next();
      final List<TestCase> path = findPath(testCase);

      for (final TestCase node : path) it.add(node);
    }
  }

  private List<TestCase> findPath(final TestCase begin) {
    final Set<TestCase> visited = new HashSet<>();
    final List<TestCase> path = new ArrayList<>();

    TestCase node = begin;

    while (node != null) {
      visited.add(node);
      final Set<TestCase> adjacentNodes = intraClassPairs.get(node);
      if (adjacentNodes == null) break;

      node = null;
      for (final TestCase adjacentNode : adjacentNodes) {
        if (visited.contains(adjacentNode)) continue;

        node = adjacentNode;
        path.add(node);
        break;
      }
    }

    return path;
  }
}
