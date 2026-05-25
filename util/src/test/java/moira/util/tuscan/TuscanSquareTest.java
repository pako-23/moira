package moira.util.tuscan;

import static moira.util.tuscan.TuscanSquareMatcher.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TuscanSquareTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 10, 30, 64, 81, 31, 23, 123})
  public void testTuscanSquareConstruction(final int n) {
    assertThat(TuscanSquare.make(n), isTuscanSquare(n));
  }
}
