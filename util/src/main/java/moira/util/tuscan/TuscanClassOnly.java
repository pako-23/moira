package moira.util.tuscan;

import moira.util.Range;
import moira.util.TestCase;
import moira.util.TestSuite;

public final class TuscanClassOnly implements SchedulesGenerator {
  private final TestSuite suite;
  private final int[][] square;
  private int index;

  public TuscanClassOnly(final TestSuite suite) {
    this.suite = suite;
    this.square = TuscanSquare.make(suite.numberOfTestClasses());
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

    for (final int classIndex : row) {
      if (classIndex == suite.numberOfTestClasses()) continue;

      final Class<?> testClass = suite.getTestClass(classIndex);
      final Range range = suite.getTestClassCases(testClass);

      for (int i = range.min(); i < range.max(); ++i, ++j) schedule[j] = suite.getTestCase(i);
    }

    return schedule;
  }
}
