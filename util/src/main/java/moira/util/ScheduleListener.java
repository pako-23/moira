package moira.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class ScheduleListener extends RunListener {
  private final List<Description> schedule;
  private final List<Boolean> outcomes;

  public ScheduleListener(final int scheduleLength) {
    schedule = new ArrayList<>(scheduleLength);
    outcomes = new ArrayList<>(scheduleLength);
  }

  @Override
  public void testStarted(final Description description) {
    schedule.add(description);
    outcomes.add(true);
  }

  @Override
  public void testFailure(final Failure failure) {
    outcomes.set(outcomes.size() - 1, false);
  }

  public void print(final PrintStream stream) {
    stream.println("Running schedule:");

    for (int i = 0; i < schedule.size(); ++i) {
      final String outcome = outcomes.get(i) ? "PASS" : "FAIL";
      stream.println("  " + TestCase.descriptionToTestID(schedule.get(i)) + " -> " + outcome);
    }
  }
}
