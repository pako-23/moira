package moira.util.tuscan;

import moira.util.Range;
import moira.util.TestSuite;

public class TuscanClassOnly extends TuscanSquare {

  public TuscanClassOnly(final TestSuite suite) {
    super(suite);
  }

  @Override
  protected int[][] buildTuscanSquare() {
    if (suite.numberOfTestClasses() % 2 == 1)
      suite.addTestClasses(moira.util.tuscan.DummyTest.class);
    final int[][] classSquare = createEvenSizeTuscanSquare(suite.numberOfTestClasses());
    final int[][] square = new int[classSquare.length][suite.numberOfTestCases()];

    for (int i = 0; i < square.length; ++i) {
      int j = 0;

      for (final int classIndex : classSquare[i]) {
        final Class<?> testClass = suite.getTestClass(classIndex);
        final Range range = suite.getTestClassCases(testClass);

        for (int k = range.min(); k < range.max(); ++k, ++j) square[i][j] = k;
      }
    }

    return square;
  }
}
