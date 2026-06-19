package moira.util.runner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import moira.util.model.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

public class JUnitResultsCollectorTest {
  @Test
  public void testAllPassed() {
    final JUnitResultsCollector listener = new JUnitResultsCollector();
    final Description first = Description.createTestDescription(getClass(), "first");
    final Description second = Description.createTestDescription(getClass(), "first");

    listener.testStarted(first);
    listener.testStarted(second);

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintStream stream = new PrintStream(buffer)) {
      listener.print(stream);
    }

    final String expected =
        String.format(
            "Running schedule:\n  %s -> PASS\n  %s -> PASS\n",
            TestCase.identifier(first.getClassName(), first.toString()),
            TestCase.identifier(second.getClassName(), second.toString()));
    assertThat(buffer.toString(), is(expected));
  }

  @Test
  public void testAllFailed() {
    final JUnitResultsCollector listener = new JUnitResultsCollector();
    final Description first = Description.createTestDescription(getClass(), "first");
    final Description second = Description.createTestDescription(getClass(), "first");

    listener.testStarted(first);
    listener.testFailure(new Failure(first, new Throwable()));
    listener.testStarted(second);
    listener.testFailure(new Failure(second, new Throwable()));

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintStream stream = new PrintStream(buffer)) {
      listener.print(stream);
    }

    final String expected =
        String.format(
            "Running schedule:\n  %s -> FAIL\n  %s -> FAIL\n",
            TestCase.identifier(first.getClassName(), first.toString()),
            TestCase.identifier(second.getClassName(), second.toString()));
    assertThat(buffer.toString(), is(expected));
  }

  @Test
  public void testOneFailed() {
    final JUnitResultsCollector listener = new JUnitResultsCollector();
    final Description first = Description.createTestDescription(getClass(), "first");
    final Description second = Description.createTestDescription(getClass(), "first");

    listener.testStarted(first);
    listener.testFailure(new Failure(first, new Throwable()));
    listener.testStarted(second);

    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintStream stream = new PrintStream(buffer)) {
      listener.print(stream);
    }

    final String expected =
        String.format(
            "Running schedule:\n  %s -> FAIL\n  %s -> PASS\n",
            TestCase.identifier(first.getClassName(), first.toString()),
            TestCase.identifier(second.getClassName(), second.toString()));
    assertThat(buffer.toString(), is(expected));
  }
}
