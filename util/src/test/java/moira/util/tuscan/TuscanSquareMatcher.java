package moira.util.tuscan;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TuscanSquareMatcher extends TypeSafeMatcher<int[][]> {
  private final int n;

  public TuscanSquareMatcher(final int n) {
    this.n = n;
  }

  @Override
  protected boolean matchesSafely(final int[][] square) {
    if (n == 0) return matchSizeZero(square);
    else if (n == 1) return matchesSizeOne(square);

    final boolean[][] pairs = new boolean[n][n];

    for (final int[] row : square) {
      for (int i = 1; i < row.length; ++i) {
        if (row[i - 1] >= n || row[i] >= n) continue;
        pairs[row[i - 1]][row[i]] = true;
      }
    }

    for (int i = 0; i < pairs.length; ++i)
      for (int j = 0; j < pairs[i].length; ++j) if (!pairs[i][j] && i != j) return false;

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("is a valid tuscan square");
  }

  public static Matcher<int[][]> isTuscanSquare(final int n) {
    return new TuscanSquareMatcher(n);
  }

  private boolean matchSizeZero(final int[][] square) {
    return square.length == 0;
  }

  private boolean matchesSizeOne(final int[][] square) {
    return square.length == 1 && square[0].length == 1 && square[0][0] == 0;
  }
}
