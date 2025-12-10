package ch.usi.inf.moira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ProfilerProxyTest {

  private ProfilerProxy proxy;

  private static class InvalidProfiler {}

  private static class DummyProfiler {
    public static String enteredTestName = null;
    public static boolean exitCalled = false;
    public static String dumpFileName = null;
    public static boolean shouldThrow = false;

    public static void reset() {
      enteredTestName = null;
      exitCalled = false;
      dumpFileName = null;
      shouldThrow = false;
    }

    public static void enterTestMethod(final String testName) {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Enter");
      }
      enteredTestName = testName;
    }

    public static void exitTestMethod() {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Exit");
      }
      exitCalled = true;
    }

    public static void dump(final String fileName) throws Throwable {
      if (shouldThrow) {
        throw new IllegalStateException("Simulated Profiler Exception on Dump");
      }
      dumpFileName = fileName;
    }
  }

  @BeforeEach
  public void setup() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    proxy = new ProfilerProxy(DummyProfiler.class.getName());
  }

  @AfterEach
  public void cleanup() {
    DummyProfiler.reset();
    System.clearProperty("moira.profiler.filename");
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
    proxy.dump();

    assertThat(DummyProfiler.dumpFileName, is("conflicts"));
  }

  @Test
  public void testCallDumpWithSystemPropertyFileName() {
    final String customFileName = "custom-report.txt";
    System.setProperty("moira.profiler.filename", customFileName);

    proxy.dump();
    assertThat(DummyProfiler.dumpFileName, is(customFileName));
  }

  @Test
  public void testDumpEmptySystemProperty() {
    System.setProperty("moira.profiler.filename", "");
    proxy.dump();
    assertThat(DummyProfiler.dumpFileName, is("conflicts"));
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
      final RuntimeException thrown = assertThrows(RuntimeException.class, () -> proxy.dump());

      assertThat(thrown.getMessage(), containsString("Failed to invoke profiler at dump"));
      assertThat(thrown.getCause(), isA(IllegalStateException.class));
    }
  }
}
