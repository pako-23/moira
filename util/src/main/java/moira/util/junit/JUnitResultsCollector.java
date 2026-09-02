package moira.util.junit;

import java.util.ArrayList;
import java.util.List;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnitResultsCollector extends RunListener {
  private final List<Outcome> outcomes;
  private TestCase runningTestCase;
  private boolean isRunningTestCasePassed;

  public JUnitResultsCollector() {
    outcomes = new ArrayList<>();
    runningTestCase = null;
    isRunningTestCasePassed = false;
  }

  @Override
  public void testStarted(final Description description) {
    runningTestCase = JUnitDescription.convert(description);
    isRunningTestCasePassed = true;
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
    outcomes.add(new Outcome(runningTestCase, isRunningTestCasePassed));
  }

  public List<Outcome> getOutcomes() {
    return outcomes;
  }
}
