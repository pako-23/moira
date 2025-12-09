package ch.usi.inf.moira.util;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class PairVerifier {
  private final Request orderedRequest;
  private final Request invertedOrderRequest;

  public PairVerifier(final TestMethod first, final TestMethod second) {
    orderedRequest = buildRequest(first, second);
    invertedOrderRequest = buildRequest(second, first);
  }

  private Request buildRequest(final TestMethod first, final TestMethod second) {
    if (first.getTestClass().equals(second.getTestClass())) {
      return singleClassRunner(first, second);
    } else {
      return multipleClassesRunner(first, second);
    }
  }

  private Request multipleClassesRunner(final TestMethod first, final TestMethod second) {
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

                final String testIdentifier = TestMethod.descriptionToTestID(description);

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            });
  }

  private Request singleClassRunner(final TestMethod first, final TestMethod second) {
    return Request.aClass(first.getTestClass())
        .filterWith(
            new Filter() {
              @Override
              public String describe() {
                return "pair filter";
              }

              @Override
              public boolean shouldRun(final Description description) {
                final String testIdentifier = TestMethod.descriptionToTestID(description);

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            })
        .sortWith((a, b) -> first.toString().equals(TestMethod.descriptionToTestID(a)) ? 1 : -1);
  }

  private boolean execute(final Request request) {
    final JUnitCore junit = new JUnitCore();
    final ScheduleListener listener = new ScheduleListener(2);

    junit.addListener(listener);
    final boolean success = junit.run(request).wasSuccessful();

    listener.print(System.out);

    return success;
  }

  public boolean verify() {
    return execute(orderedRequest) ^ execute(invertedOrderRequest);
  }
}
