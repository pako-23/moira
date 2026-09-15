package moira.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.junit.JUnitRunner;
import moira.model.Outcome;
import moira.model.TestCase;
import moira.runner.TestRunner;

public class ScheduleExecutor {

  private ScheduleExecutor() {}

  public static void main(final String... args) throws IOException {
    final PrintStream stdout = System.out;

    System.setOut(System.err);
    run(System.in, stdout);
    System.exit(0);
  }

  public static void run(final InputStream input, final PrintStream output) throws IOException {
    final List<TestCase> schedule = readSchedule(input);

    final List<Outcome> outcomes = executeSchedule(schedule);

    for (final Outcome outcome : outcomes) output.println(outcome.pass());
  }

  private static List<TestCase> readSchedule(final InputStream input) throws IOException {
    final List<TestCase> schedule = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;

      while ((line = reader.readLine()) != null) schedule.add(TestCase.fromId(line));
    }

    return schedule;
  }

  private static List<Outcome> executeSchedule(final List<TestCase> schedule) {
    final List<AbstractMap.SimpleEntry<String, Set<String>>> testClasses = getTestClasses(schedule);
    final Map<String, Integer> order = getTestIdToIndexMapping(schedule);
    final TestRunner runner = new JUnitRunner();

    return runner
        .request(testClasses.stream().map(AbstractMap.SimpleEntry::getKey).toArray(String[]::new))
        .withFilter(
            new Predicate<TestCase>() {
              private int lastIndex = 0;

              @Override
              public boolean test(final TestCase test) {
                if (lastIndex >= testClasses.size()) return false;

                final String testId = test.toString();
                final Set<String> tests = testClasses.get(lastIndex).getValue();

                if (!tests.contains(testId)) return false;

                tests.remove(testId);
                if (tests.size() == 0) ++lastIndex;

                return true;
              }
            })
        .withOrder((a, b) -> order.get(a.toString()) - order.get(b.toString()))
        .run();
  }

  private static List<AbstractMap.SimpleEntry<String, Set<String>>> getTestClasses(
      final List<TestCase> schedule) {
    final List<AbstractMap.SimpleEntry<String, Set<String>>> testClasses =
        new ArrayList<>(schedule.size());

    testClasses.add(
        new AbstractMap.SimpleEntry<String, Set<String>>(
            schedule.get(0).getTestClass(),
            Stream.of(schedule.get(0).toString()).collect(Collectors.toSet())));

    for (int i = 1; i < schedule.size(); ++i) {
      final TestCase method = schedule.get(i);
      final AbstractMap.SimpleEntry<String, Set<String>> pair =
          testClasses.get(testClasses.size() - 1);

      if (method.getTestClass().equals(pair.getKey())) pair.getValue().add(method.toString());
      else
        testClasses.add(
            new AbstractMap.SimpleEntry<String, Set<String>>(
                method.getTestClass(), Stream.of(method.toString()).collect(Collectors.toSet())));
    }

    return testClasses;
  }

  private static Map<String, Integer> getTestIdToIndexMapping(final List<TestCase> schedule) {
    final Map<String, Integer> order = new HashMap<>();
    for (int i = 0; i < schedule.size(); ++i) order.put(schedule.get(i).toString(), i);

    return order;
  }
}
