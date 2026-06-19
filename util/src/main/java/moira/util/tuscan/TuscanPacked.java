package moira.util.tuscan;

import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.runner.ScheduleGenerator;

public final class TuscanPacked implements ScheduleGenerator {
  private final TestSuite suite;
  private final int[][] square;
  private int index;

  public TuscanPacked(final TestSuite suite) {
    this.suite = suite;
    this.square = TuscanSquare.make(suite.numberOfTestCases());
    this.index = 0;
  }

  @Override
  public boolean done() {
    return index >= square.length;
  }

  @Override
  public TestCase[] generate() {
    final int[] row = square[index++];
    final TestCase[] schedule = new TestCase[suite.numberOfTestCases()];

    int j = 0;
    for (final int i : row) {
      if (i == suite.numberOfTestCases()) continue;
      schedule[j++] = suite.getTestCase(i);
    }

    return schedule;
  }

  @Override
  public int count() {
    return square.length;
  }
}
