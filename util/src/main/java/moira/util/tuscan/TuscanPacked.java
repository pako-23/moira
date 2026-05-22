package moira.util.tuscan;

import moira.util.TestSuite;

public class TuscanPacked extends TuscanSquare {
  public TuscanPacked(final TestSuite suite) {
    super(suite);
  }

  @Override
  protected int[][] buildTuscanSquare() {
    if (suite.numberOfTestCases() % 2 == 1) suite.addTestClasses(moira.util.tuscan.DummyTest.class);
    return createEvenSizeTuscanSquare(suite.numberOfTestCases());
  }
}
