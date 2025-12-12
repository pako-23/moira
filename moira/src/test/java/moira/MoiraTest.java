package moira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class MoiraTest {
  @AfterEach
  public void cleanup() {
    System.clearProperty("moira.profiler.name");
  }

  private static Stream<Arguments> testSimpleExecutionParams() {
    return Stream.of("", null, "ObjectProfiler")
        .flatMap(
            profiler -> Stream.of(true, false).map(success -> Arguments.of(profiler, success)));
  }

  @ParameterizedTest
  @MethodSource("testSimpleExecutionParams")
  public void testSimpleExecution(final String profiler, final boolean success) {
    final Result result = mock(Result.class);
    when(result.wasSuccessful()).thenReturn(success);
    if (profiler != null) System.setProperty("moira.profiler.name", profiler);

    try (final MockedStatic<Request> requestMock = mockStatic(Request.class);
        final MockedConstruction<JUnitCore> junitMock =
            mockConstruction(
                JUnitCore.class,
                (mock, context) -> {
                  when(mock.run(any(Request.class))).thenReturn(result);
                });
        final MockedConstruction<ProfilerProxy> proxyMock =
            mockConstruction(
                ProfilerProxy.class,
                (mock, context) -> {
                  if (profiler == null || profiler.isEmpty())
                    assertThat(context.arguments().get(0), is("moira.profiler.NullProfiler"));
                  else assertThat(context.arguments().get(0), is("moira.profiler." + profiler));
                })) {
      final Request returnedRequest = mock(Request.class);
      requestMock.when(() -> Request.classes(any())).thenReturn(returnedRequest);
      final int exitCode = new Moira().run(MoiraTest.class.getName());
      if (success) assertThat(exitCode, is(0));
      else assertThat(exitCode, not(is(0)));
    }
  }

  @Test
  public void testNotValidTestClass() {
    assertThat(new Moira().run("some.not.existing.Class"), not(is(0)));
  }
}
