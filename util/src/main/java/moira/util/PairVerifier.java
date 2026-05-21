package moira.util;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class PairVerifier {
  private final Request request;

  public PairVerifier(final TestCase first, final TestCase second) {
    request = buildRequest(first, second);
  }

  private Request buildRequest(final TestCase first, final TestCase second) {
    if (first.getTestClass().equals(second.getTestClass())) {
      return singleClassRunner(first, second);
    } else {
      return multipleClassesRunner(first, second);
    }
  }

  private Request multipleClassesRunner(final TestCase first, final TestCase second) {
    return Request.classes(first.getTestClass(), second.getTestClass())
        .filterWith(
            new Filter() {
              @Override
              public String describe() {
                return "pair filter";
              }

              @Override
              public boolean shouldRun(final Description description) {
                if (description.isSuite()) return true;

                final String testIdentifier = TestCase.descriptionToTestID(description);

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            });
  }

  private Request singleClassRunner(final TestCase first, final TestCase second) {
    return Request.aClass(first.getTestClass())
        .filterWith(
            new Filter() {
              @Override
              public String describe() {
                return "pair filter";
              }

              @Override
              public boolean shouldRun(final Description description) {
                final String testIdentifier = TestCase.descriptionToTestID(description);

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            })
        .sortWith((a, b) -> first.toString().equals(TestCase.descriptionToTestID(a)) ? 1 : -1);
  }

  public boolean verify() {
    final JUnitCore junit = new JUnitCore();
    final ScheduleListener listener = new ScheduleListener(2);

    junit.addListener(listener);
    final boolean success = junit.run(request).wasSuccessful();

    listener.print(System.out);

    return success;
  }
}
