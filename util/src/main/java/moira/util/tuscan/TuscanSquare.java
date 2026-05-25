package moira.util.tuscan;

public final class TuscanSquare {
  private TuscanSquare() {}

  public static int[][] make(final int n) {
    if (n == 1) return new int[][] {{0}};
    else if (n % 2 == 1) return makeEvenSize(n + 1);
    else return makeEvenSize(n);
  }

  private static int[][] makeEvenSize(final int n) {
    if (n == 0) return new int[][] {};

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
