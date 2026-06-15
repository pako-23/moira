package moira.util.tuscan;

import java.util.ArrayList;
import java.util.List;
import moira.util.Range;
import moira.util.TestCase;
import moira.util.TestSuite;
import moira.util.runner.ScheduleGenerator;

public class TuscanIntraClass implements ScheduleGenerator {
  private final TestSuite suite;
  private final int[][] classOnlySquare;
  private final List<int[][]> intraClassSquares;
  private int iteration;
  private int size;

  public TuscanIntraClass(final TestSuite suite) {
    this.suite = suite;
    this.classOnlySquare = TuscanSquare.make(suite.numberOfTestClasses());

    this.intraClassSquares = new ArrayList<>(suite.numberOfTestClasses());
    for (int i = 0; i < suite.numberOfTestClasses(); ++i) {
      final Class<?> testClass = suite.getTestClass(i);
      final Range range = suite.getTestClassCases(testClass);
      final int length = range.max() - range.min();
      final int[][] intraClassSquare = TuscanSquare.make(length);
      for (int j = 0; j < intraClassSquare.length; ++j)
        for (int k = 0; k < intraClassSquare[j].length; ++k) intraClassSquare[j][k] += range.min();

      intraClassSquares.add(intraClassSquare);
    }

    this.iteration = 0;
    this.size = classOnlySquare.length;
    for (final int[][] square : intraClassSquares)
      if (square.length > this.size) this.size = square.length;
  }

  @Override
  public boolean done() {
    return iteration >= size;
  }

  @Override
  public TestCase[] generate() {
    final int[] classRow = classOnlySquare[iteration % classOnlySquare.length];
    final List<TestCase> schedule = new ArrayList<>(suite.numberOfTestCases());

    for (final int classIndex : classRow) {
      if (classIndex == suite.numberOfTestClasses()) continue;

      final Class<?> testClass = suite.getTestClass(classIndex);
      final Range range = suite.getTestClassCases(testClass);
      final int[][] intraClassSquare = intraClassSquares.get(classIndex);
      if (intraClassSquare.length == 0) continue;
      final int[] intraClassRow = intraClassSquare[iteration % intraClassSquare.length];

      for (final int index : intraClassRow) {
        if (index == range.max()) continue;
        schedule.add(suite.getTestCase(index));
      }
    }

    ++iteration;
    return schedule.stream().toArray(TestCase[]::new);
  }
}
