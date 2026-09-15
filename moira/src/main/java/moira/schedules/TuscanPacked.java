package moira.schedules;

import moira.model.TestCase;
import moira.model.TestSuite;

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
