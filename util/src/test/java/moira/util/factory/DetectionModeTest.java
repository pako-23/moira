package moira.util.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DetectionModeTest {

  @ParameterizedTest
  @MethodSource("provideDetectionModeTextPairs")
  public void testToString(final DetectionMode mode, final String value) {
    assertThat(mode.toString(), is(value));
  }

  @ParameterizedTest
  @MethodSource("provideDetectionModeTextPairs")
  public void testFromString(final DetectionMode mode, final String value) {
    assertThat(DetectionMode.fromString(value), is(mode));
  }

  @Test
  public void testFromStringInvalid() {
    assertThat(DetectionMode.fromString("something"), nullValue());
  }

  private static Stream<Arguments> provideDetectionModeTextPairs() {
    return Stream.of(
        Arguments.of(DetectionMode.TUSCAN_PACKED, "tuscan-packed"),
        Arguments.of(DetectionMode.TUSCAN_CLASS_ONLY, "tuscan-class-only"),
        Arguments.of(DetectionMode.TUSCAN_INTRA_CLASS, "tuscan-intra-class"),
        Arguments.of(DetectionMode.TUSCAN_INTER_CLASS, "tuscan-inter-class"),
        Arguments.of(DetectionMode.TARGET_PAIRS, "target-pairs"),
        Arguments.of(DetectionMode.MOIRA, "moira"));
  }
}
