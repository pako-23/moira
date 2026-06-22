package moira.util.tuscan;

import moira.util.model.Range;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.runner.ScheduleGenerator;

public final class TuscanClassOnly implements ScheduleGenerator {
  private final TestSuite suite;
  private final int[][] square;
  private int index;

  public TuscanClassOnly(final TestSuite suite) {
    this.suite = suite;
    this.square = TuscanSquare.make(suite.numberOfTestClasses());
    this.index = 0;
  }

  @Override
  public TestCase[] generate() {
    final int[] row = square[index++];

    final TestCase[] schedule = new TestCase[suite.numberOfTestCases()];
    int j = 0;

    for (final int classIndex : row) {
      if (classIndex == suite.numberOfTestClasses()) continue;

      final String testClass = suite.getTestClass(classIndex);
      final Range range = suite.getTestClassCases(testClass);

      for (int i = range.min(); i < range.max(); ++i, ++j) schedule[j] = suite.getTestCase(i);
    }

    return schedule;
  }

  @Override
  public int count() {
    return square.length;
  }
}
