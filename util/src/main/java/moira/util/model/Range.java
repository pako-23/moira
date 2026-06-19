package moira.util.model;

public class Range {
  private final int lowerBound;
  private final int upperBound;

  public Range(final int lowerBound, final int upperBound) {
    this.lowerBound = lowerBound;
    this.upperBound = upperBound;
  }

  public int min() {
    return lowerBound;
  }

  public int max() {
    return upperBound;
  }
}
