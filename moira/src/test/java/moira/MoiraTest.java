package moira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
      new Moira().run(MoiraTest.class.getName());
      assertThat(proxyMock.constructed().size(), is(1));
      if (success) verify(proxyMock.constructed().get(0)).dump();
      else verify(proxyMock.constructed().get(0), never()).dump();
    }
  }

  @Test
  public void testNotValidTestClass() {
    final PrintStream originalStderr = System.err;
    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      System.setErr(new PrintStream(stream));
      new Moira().run("some.not.existing.Class");
    } finally {
      System.setErr(originalStderr);
    }

    assertThat(stream.toByteArray().length, not(is(0)));
  }
}
