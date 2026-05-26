package moira.util.tuscan;

import java.util.ArrayList;
import java.util.List;
import moira.util.Range;
import moira.util.TestCase;
import moira.util.TestSuite;

public final class TuscanInterClass implements SchedulesGenerator {
  private final TestSuite suite;
  private final int[][] classOnlySquare;
  private final List<int[][]> intraClassSquares;
  private int classOnlyRowIndex;

  private int classOnlyColumnIndex;
  private int classOnlyNextColumnIndex;

  private int intraClassRowIndex;
  private int nextIntraClassRowIndex;

  public TuscanInterClass(final TestSuite suite) {
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

    this.classOnlyRowIndex = -1;
    advanceClassOnlyEndOfRow();

    this.intraClassRowIndex = 0;
    this.nextIntraClassRowIndex = 0;
  }

  @Override
  public boolean done() {
    return classOnlyRowIndex >= classOnlySquare.length;
  }

  @Override
  public TestCase[] generate() {
    final List<TestCase> schedule =
        produceSchedule(this.classOnlyColumnIndex, this.intraClassRowIndex);
    if (classOnlyNextColumnIndex != -1)
      schedule.addAll(produceSchedule(this.classOnlyNextColumnIndex, this.nextIntraClassRowIndex));

    advance();

    return schedule.stream().toArray(TestCase[]::new);
  }

  private List<TestCase> produceSchedule(
      final int classOnlyColumnIndex, final int intraClassRowIndex) {
    final int[] classOnlyRow = classOnlySquare[classOnlyRowIndex];
    final int testClassIndex = classOnlyRow[classOnlyColumnIndex];
    final int[] testCases = intraClassSquares.get(testClassIndex)[intraClassRowIndex];
    final Class<?> testClass = suite.getTestClass(testClassIndex);
    final Range range = suite.getTestClassCases(testClass);
    final List<TestCase> schedule = new ArrayList<>(testCases.length);

    for (final int index : testCases) {
      if (index == range.max()) continue;
      schedule.add(suite.getTestCase(index));
    }

    return schedule;
  }

  private void advance() {
    final int[] classOnlyRow = classOnlySquare[classOnlyRowIndex];
    final int testClassIndex = classOnlyRow[classOnlyColumnIndex];
    final int[][] testClassIntraClassSquare = intraClassSquares.get(testClassIndex);

    if (++intraClassRowIndex < testClassIntraClassSquare.length) return;

    intraClassRowIndex = 0;

    if (classOnlyNextColumnIndex == -1) {
      advanceClassOnly();
      return;
    }

    final int nextTestClassIndex = classOnlyRow[classOnlyNextColumnIndex];
    final int[][] nextTestClassIntraClassSquare = intraClassSquares.get(nextTestClassIndex);

    if (++nextIntraClassRowIndex < nextTestClassIntraClassSquare.length) return;

    nextIntraClassRowIndex = 0;
    advanceClassOnly();
  }

  private void advanceClassOnly() {
    if (classOnlyNextColumnIndex == -1) {
      advanceClassOnlyEndOfRow();
      return;
    }

    classOnlyColumnIndex = classOnlyNextColumnIndex;
    classOnlyNextColumnIndex = findFirstValidTestClassIndex(classOnlyColumnIndex + 1);
  }

  private void advanceClassOnlyEndOfRow() {
    if (++classOnlyRowIndex >= classOnlySquare.length) return;

    classOnlyColumnIndex = findFirstValidTestClassIndex(0);
    classOnlyNextColumnIndex = findFirstValidTestClassIndex(1);
  }

  private int findFirstValidTestClassIndex(int index) {
    final int[] row = classOnlySquare[classOnlyRowIndex];

    while (index < row.length && !isValidTestClass(row[index])) ++index;
    if (index >= row.length) return -1;

    return index;
  }

  private boolean isValidTestClass(final int classIndex) {
    return classIndex < suite.numberOfTestClasses() && !isEmptyTestClass(classIndex);
  }

  private boolean isEmptyTestClass(final int index) {
    final Class<?> testClass = suite.getTestClass(index);
    final Range range = suite.getTestClassCases(testClass);

    return range.max() == range.min();
  }
}
