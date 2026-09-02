package moira.util.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.junit.JUnitRunner;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public class ChildRunner {
  // public static void main(String[] args) throws IOException {
  //   final List<TestCase> tests = new ArrayList<>();
  //   try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
  //     String line;
  //     while ((line = reader.readLine()) != null) tests.add(TestCase.fromId(line));
  //   }

  //   final PrintStream originalOut = System.out;
  //   System.setOut(System.err);

  //   final List<Boolean> results = new JUnitExecutor(tests).run();
  //   System.setOut(originalOut);

  //   for (final boolean result : results) System.out.println(result);

  //   System.exit(0);
  // }

  private final TestRunner runner;

  public ChildRunner(final TestRunner runner) {
    this.runner = runner;
  }

  public void run(final InputStream input, final OutputStream output) throws IOException {
    final List<TestCase> schedule = readSchedule(input);
    final List<Outcome> outcomes = runSchedule(schedule);

    outputOutcomes(output, outcomes);
  }

  private static List<TestCase> readSchedule(final InputStream intput) throws IOException {
    final List<TestCase> schedule = new ArrayList<>();
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) schedule.add(TestCase.fromId(line));
    }

    return schedule;
  }

  private static void outputOutcomes(final OutputStream output, final List<Outcome> outcomes) {
    final PrintStream stream = new PrintStream(output);

    for (final Outcome outcome : outcomes) stream.println(outcome.pass());
  }

  private List<Outcome> runSchedule(final List<TestCase> schedule) {
    final List<AbstractMap.SimpleEntry<String, Set<String>>> testClasses = getTestClasses(schedule);
    final Map<String, Integer> order = getTestIdToIndexMapping(schedule);

    return runner
        .request(testClasses.stream().map(AbstractMap.SimpleEntry::getKey).toArray(String[]::new))
        .run();
  }

  private List<AbstractMap.SimpleEntry<String, Set<String>>> getTestClasses(
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

  private Map<String, Integer> getTestIdToIndexMapping(final List<TestCase> schedule) {
    final Map<String, Integer> order = new HashMap<>();
    for (int i = 0; i < schedule.size(); ++i) order.put(schedule.get(i).toString(), i);

    return order;
  }

  public static void main(final String[] args) throws IOException {
    final PrintStream stdout = System.out;

    System.setOut(System.err);
    final ChildRunner runner = new ChildRunner(new JUnitRunner());
    runner.run(System.in, stdout);
  }
}
