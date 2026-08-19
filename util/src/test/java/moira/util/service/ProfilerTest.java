package moira.util.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProfilerTest {

  @ParameterizedTest
  @MethodSource("provideProfilerTextPairs")
  public void testToString(final Profiler profiler, final String value) {
    assertThat(profiler.toString(), is(value));
  }

  @ParameterizedTest
  @MethodSource("provideProfilerTextPairs")
  public void testFromString(final Profiler profiler, final String value) {
    assertThat(Profiler.fromString(value), is(profiler));
  }

  @Test
  public void testFromStringInvalid() {
    assertThat(Profiler.fromString("something"), nullValue());
  }

  @ParameterizedTest
  @MethodSource("provideProfilerClassPairs")
  public void testGetProfilerClass(final Profiler profiler, final String expected) {
    assertThat(profiler.getProfilerClass(), is(expected));
  }

  private static Stream<Arguments> provideProfilerTextPairs() {
    return Stream.of(
        Arguments.of(Profiler.ONLINE, "online"),
        Arguments.of(Profiler.NAIVE, "naive"),
        Arguments.of(Profiler.OBJECT, "object"),
        Arguments.of(Profiler.TARGET_PAIRS, "target-pairs"),
        Arguments.of(Profiler.NULL, "null"));
  }

  private static Stream<Arguments> provideProfilerClassPairs() {
    return Stream.of(
        Arguments.of(Profiler.ONLINE, "OnlineProfiler"),
        Arguments.of(Profiler.NAIVE, "NaiveProfiler"),
        Arguments.of(Profiler.OBJECT, "ObjectProfiler"),
        Arguments.of(Profiler.TARGET_PAIRS, "TargetPairsProfiler"),
        Arguments.of(Profiler.NULL, "NullProfiler"));
  }
}
