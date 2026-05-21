package moira.util.tuscan;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import moira.util.TestCase;
import moira.util.TestSuite;
import moira.util.runner.ScheduleRunner;

public class TuscanClassOnly {
  private final List<TestCase> cases;
  private final int[][] square;

  public TuscanClassOnly(final TestSuite suite) {
    if (suite.testClassesSize() % 2 == 1) suite.addTestClasses(moira.util.tuscan.DummyTest.class);

    final int suiteSize = suite.size();
    final List<Class<?>> classes = suite.getTestClasses();
    final int[][] classSquare = buildTuscanSquare(classes.size());

    cases = new ArrayList<>(suiteSize);
    square = new int[classSquare.length][suiteSize];

    final List<AbstractMap.SimpleEntry<Integer, Integer>> ranges = new ArrayList<>(classes.size());
    int base = 0;

    for (final Class<?> testClass : classes) {
      final List<TestCase> testClassCases = suite.getClassTestCases(testClass);
      ranges.add(new AbstractMap.SimpleEntry<Integer, Integer>(base, base + testClassCases.size()));
      base += testClassCases.size();
      cases.addAll(testClassCases);
    }

    for (int i = 0; i < square.length; ++i) {
      int j = 0;

      for (final int value : classSquare[i]) {
        final AbstractMap.SimpleEntry<Integer, Integer> range = ranges.get(value);
        int min = range.getKey();
        final int max = range.getValue();

        for (; min < max; ++min, ++j) square[i][j] = min;
      }
    }
  }

  public void run(final ScheduleRunner runner) throws ExecutionException, InterruptedException {
    final List<boolean[]> results = submitSuitesExecutions(runner);

    final Set<Integer> failingInIsolation = findFailingInIsolation(results);
    final Map<Integer, Integer> brittle = new HashMap<>();
    final Map<Integer, Integer> victims = new HashMap<>();

    for (int i = 0; i < results.size(); ++i) {
      final boolean[] outcome = results.get(i);

      for (int j = 1; j < outcome.length; ++j) {
        final int test = square[i][j];
        final int previousTest = square[i][j - 1];

        if (outcome[j] && failingInIsolation.contains(test)) brittle.put(test, previousTest);
        else if (!outcome[j]) victims.put(test, previousTest);
      }
    }

    for (final Map.Entry<Integer, Integer> pair : brittle.entrySet())
      System.out.printf(
          "from: %s, to: %s, type: brittle\n",
          cases.get(pair.getKey()).toString(), cases.get(pair.getValue()).toString());

    for (final Map.Entry<Integer, Integer> pair : victims.entrySet())
      System.out.printf(
          "from: %s, to: %s, type: victim\n",
          cases.get(pair.getKey()).toString(), cases.get(pair.getValue()).toString());
  }

  private int[][] buildTuscanSquare(final int n) {
    if (n % 2 == 1)
      throw new IllegalArgumentException("tuscan square can be build only for even lengths");

    final int[][] square = new int[n][n];

    for (int i = 0; i < n; i += 2) {
      square[0][i] = i / 2;
      square[0][i + 1] = n - 1 - square[0][i];
    }

    for (int i = 1; i < n; ++i)
      for (int j = 0; j < n; ++j) square[i][j] = (square[i - 1][j] + 1) % n;

    return square;
  }

  private List<boolean[]> submitSuitesExecutions(final ScheduleRunner runner)
      throws ExecutionException, InterruptedException {
    final List<boolean[]> results = new ArrayList<>(square.length);
    final List<Future<boolean[]>> jobs = new ArrayList<>(square.length);

    for (final int[] row : square) {
      final TestCase[] schedule = new TestCase[row.length];

      for (int i = 0; i < row.length; ++i) schedule[i] = cases.get(row[i]);

      jobs.add(runner.submit(schedule));
    }

    for (final Future<boolean[]> job : jobs) results.add(job.get());

    return results;
  }

  private Set<Integer> findFailingInIsolation(final List<boolean[]> results) {
    final Set<Integer> brittle = new HashSet<>();
    for (int i = 0; i < results.size(); ++i) {
      final boolean[] outcome = results.get(i);
      if (!outcome[0]) brittle.add(square[i][0]);
    }

    return brittle;
  }
}
