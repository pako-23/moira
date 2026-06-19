package moira.util.runner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.model.TestCase;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnitResultsCollector extends RunListener {
  private final List<String> schedule;
  private final List<Boolean> outcomes;

  public JUnitResultsCollector() {
    schedule = new ArrayList<>();
    outcomes = new ArrayList<>();
  }

  @Override
  public void testStarted(final Description description) {
    schedule.add(TestCase.identifier(description.getClassName(), description.toString()));
    outcomes.add(true);
  }

  @Override
  public void testFailure(final Failure failure) {
    outcomes.set(outcomes.size() - 1, false);
  }

  public List<Boolean> getOutcomes() {
    return outcomes;
  }

  public void print(final PrintStream stream) {
    stream.println("Running schedule:");

    for (int i = 0; i < schedule.size(); ++i) {
      final String outcome = outcomes.get(i) ? "PASS" : "FAIL";
      stream.println("  " + schedule.get(i) + " -> " + outcome);
    }
  }
}
