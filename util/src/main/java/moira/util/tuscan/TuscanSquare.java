package moira.util.tuscan;

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

public abstract class TuscanSquare {
  protected final TestSuite suite;
  protected final int[][] square;

  public TuscanSquare(final TestSuite suite) {
    this.suite = suite;
    this.square = this.buildTuscanSquare();
  }

  protected abstract int[][] buildTuscanSquare();

  protected int[][] createEvenSizeTuscanSquare(final int n) {
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
          suite.getTestCase(pair.getKey()).toString(),
          suite.getTestCase(pair.getValue()).toString());

    for (final Map.Entry<Integer, Integer> pair : victims.entrySet())
      System.out.printf(
          "from: %s, to: %s, type: victim\n",
          suite.getTestCase(pair.getKey()).toString(),
          suite.getTestCase(pair.getValue()).toString());
  }

  private List<boolean[]> submitSuitesExecutions(final ScheduleRunner runner)
      throws ExecutionException, InterruptedException {
    final List<boolean[]> results = new ArrayList<>(square.length);
    final List<Future<boolean[]>> jobs = new ArrayList<>(square.length);

    for (final int[] row : square) {
      final TestCase[] schedule = new TestCase[row.length];

      for (int i = 0; i < row.length; ++i) schedule[i] = suite.getTestCase(row[i]);

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
