package moira.util.tuscan;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import moira.util.TestCase;
import moira.util.TestSuite;

public class TuscanClassOnly {
  private final List<TestCase> cases;
  private final int[][] square;

  public TuscanClassOnly(final TestSuite suite) {
    if (suite.testClassesSize() % 2 == 0) suite.addTestClasses(moira.util.tuscan.DummyTest.class);

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
}
