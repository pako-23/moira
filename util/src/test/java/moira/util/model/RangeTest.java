package moira.util.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RangeTest {
  @ParameterizedTest
  @MethodSource("testRangeBoundsParams")
  public void testRangeBounds(final int lower, final int upper) {
    final Range range = new Range(lower, upper);
    assertThat(range.min(), is(lower));
    assertThat(range.max(), is(upper));
  }

  private static Stream<Arguments> testRangeBoundsParams() {
    return Stream.of(
        Arguments.of(0, 0),
        Arguments.of(0, 1),
        Arguments.of(1, 10),
        Arguments.of(-5, 5),
        Arguments.of(-10, -1),
        Arguments.of(Integer.MIN_VALUE, Integer.MAX_VALUE));
  }

  @Test
  public void testMinReturnsLowerBound() {
    final Range range = new Range(3, 7);
    assertThat(range.min(), is(3));
  }

  @Test
  public void testMaxReturnsUpperBound() {
    final Range range = new Range(3, 7);
    assertThat(range.max(), is(7));
  }

  @Test
  public void testEqualBounds() {
    final Range range = new Range(5, 5);
    assertThat(range.min(), is(5));
    assertThat(range.max(), is(5));
  }

  @Test
  public void testNegativeBounds() {
    final Range range = new Range(-10, -1);
    assertThat(range.min(), is(-10));
    assertThat(range.max(), is(-1));
  }

  @Test
  public void testZeroBounds() {
    final Range range = new Range(0, 0);
    assertThat(range.min(), is(0));
    assertThat(range.max(), is(0));
  }

  @Test
  public void testInvalidBounds() {
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new Range(5, 3));
    assertThat(thrown.getMessage(), is("upperBound (3) must be >= lowerBound (5)"));
  }
}
