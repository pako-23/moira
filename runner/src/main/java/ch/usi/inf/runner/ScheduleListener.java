package ch.usi.inf.runner;

import java.util.HashSet;
import java.util.Set;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class ScheduleListener extends RunListener {
  private final String[] tests;
  private final Set<String> failed;
  private final Set<String> ignored;

  public ScheduleListener(String... tests) {
    this.tests = tests;
    failed = new HashSet<>();
    ignored = new HashSet<>();
  }

  @Override
  public void testFailure(Failure failure) throws Exception {
    failed.add(failure.getDescription().getDisplayName());
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    ignored.add(description.getDisplayName());
  }

  public String[] getTestsOutcome() {
    final String[] outcome = new String[tests.length];

    for (int i = 0; i < tests.length; ++i) {
      String[] parts = tests[i].split("#");
      String className = parts[0];
      String methodName = parts[1];
      String description = methodName + "(" + className + ")";

      if (ignored.contains(description)) outcome[i] = "SKIP";
      else if (failed.contains(description)) outcome[i] = "FAIL";
      else outcome[i] = "PASS";
    }

    return outcome;
  }
}
