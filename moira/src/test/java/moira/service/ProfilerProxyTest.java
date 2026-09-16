package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ProfilerProxyTest {
  private static final PrintStream output =
      new PrintStream(
          new OutputStream() {
            @Override
            public void write(int arg0) throws IOException {}
          });

  private ProfilerProxy proxy;

  private static class InvalidProfiler {}

  private static class DummyProfiler {
    public static String enteredTestName = null;
    public static boolean exitCalled = false;
    public static PrintStream dumpOutputStream = null;
    public static boolean shouldThrow = false;

    public static void reset() {
      enteredTestName = null;
      exitCalled = false;
      dumpOutputStream = null;
      shouldThrow = false;
    }

    @SuppressWarnings("unused")
    public static void enterTestMethod(final String testName) {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Enter");
      }
      enteredTestName = testName;
    }

    @SuppressWarnings("unused")
    public static void exitTestMethod() {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Exit");
      }
      exitCalled = true;
    }

    @SuppressWarnings("unused")
    public static void dump(final PrintStream output) throws Throwable {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Dump");
      }
      dumpOutputStream = output;
    }
  }

  @BeforeEach
  public void setup() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    proxy = new ProfilerProxy(DummyProfiler.class.getName());
  }

  @AfterEach
  public void cleanup() {
    DummyProfiler.reset();
  }

  @Nested
  public class ConstructorTests {
    @Test
    public void testValidProfiler() {
      assertDoesNotThrow(() -> new ProfilerProxy(DummyProfiler.class.getName()));
    }

    @Test
    public void testInvalidValidProfiler() {
      assertThrows(
          NoSuchMethodException.class, () -> new ProfilerProxy(InvalidProfiler.class.getName()));
    }
  }

  @Test
  public void testSuccessfullyCallEnterTestMethod() {
    final String testName = "myTest";
    proxy.enterTestMethod(testName);

    assertThat(testName, is(DummyProfiler.enteredTestName));
  }

  @Test
  public void testSuccessfullyCallExitTestMethod() {
    proxy.exitTestMethod();

    assertThat(DummyProfiler.exitCalled, is(true));
  }

  @Test
  public void testCallDumpWithDefaultFileName() {
    proxy.dump(output);

    assertThat(DummyProfiler.dumpOutputStream, is(output));
  }

  @Nested
  public class ExceptionTests {
    @BeforeEach
    public void setup() {
      DummyProfiler.shouldThrow = true;
    }

    @Test
    public void testExceptionInEnterTestMethod() {
      final String testName = "myTest";
      final RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> proxy.enterTestMethod(testName));

      assertThat(thrown.getMessage(), containsString("Failed to invoke profiler at test enter"));
      assertThat(thrown.getCause(), isA(IllegalStateException.class));
    }

    @Test
    public void testExceptionInExitTestMethod() {
      final RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> proxy.exitTestMethod());

      assertThat(thrown.getMessage(), containsString("Failed to invoke profiler at test exit"));
      assertThat(thrown.getCause(), isA(IllegalStateException.class));
    }

    @Test
    public void testExceptionInDump() {
      final RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> proxy.dump(output));

      assertThat(thrown.getMessage(), containsString("Failed to invoke profiler at dump"));
      assertThat(thrown.getCause(), isA(IllegalStateException.class));
    }
  }
}
