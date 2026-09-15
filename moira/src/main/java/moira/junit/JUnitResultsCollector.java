package moira.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import moira.model.Outcome;
import moira.model.TestCase;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnitResultsCollector extends RunListener {
  private final List<Outcome> outcomes;
  private TestCase runningTestCase;
  private boolean isRunningTestCasePassed;
  private final Consumer<TestCase> testStartedCallback;
  private final Consumer<TestCase> testFinishedCallback;

  public JUnitResultsCollector(
      final Consumer<TestCase> testStartedCallback, final Consumer<TestCase> testFinishedCallback) {
    outcomes = new ArrayList<>();
    runningTestCase = null;
    isRunningTestCasePassed = false;
    this.testStartedCallback = testStartedCallback;
    this.testFinishedCallback = testFinishedCallback;
  }

  @Override
  public void testStarted(final Description description) {
    runningTestCase = JUnitDescription.convert(description);
    isRunningTestCasePassed = true;
    testStartedCallback.accept(runningTestCase);
  }

  @Override
  public void testIgnored(final Description description) {
    outcomes.add(new Outcome(JUnitDescription.convert(description), true));
  }

  @Override
  public void testFailure(final Failure failure) {
    isRunningTestCasePassed = false;
  }

  @Override
  public void testFinished(final Description description) {
    testFinishedCallback.accept(runningTestCase);
    outcomes.add(new Outcome(runningTestCase, isRunningTestCasePassed));
  }

  public List<Outcome> getOutcomes() {
    return outcomes;
  }
}
