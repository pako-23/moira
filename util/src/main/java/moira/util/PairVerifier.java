package moira.util;

import moira.util.model.TestCase;
import moira.util.runner.JUnitResultsCollector;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class PairVerifier {
  private final Request request;

  public PairVerifier(final TestCase first, final TestCase second) throws ClassNotFoundException {
    request = buildRequest(first, second);
  }

  private Request buildRequest(final TestCase first, final TestCase second)
      throws ClassNotFoundException {
    if (first.getTestClass().equals(second.getTestClass())) {
      return singleClassRunner(first, second);
    } else {
      return multipleClassesRunner(first, second);
    }
  }

  private Request multipleClassesRunner(final TestCase first, final TestCase second)
      throws ClassNotFoundException {
    return Request.classes(
            Class.forName(first.getTestClass()), Class.forName(second.getTestClass()))
        .filterWith(
            new Filter() {
              @Override
              public String describe() {
                return "pair filter";
              }

              @Override
              public boolean shouldRun(final Description description) {
                if (description.isSuite()) return true;

                final String testIdentifier =
                    TestCase.identifier(description.getClassName(), description.toString());

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            });
  }

  private Request singleClassRunner(final TestCase first, final TestCase second)
      throws ClassNotFoundException {
    return Request.aClass(Class.forName(first.getTestClass()))
        .filterWith(
            new Filter() {
              @Override
              public String describe() {
                return "pair filter";
              }

              @Override
              public boolean shouldRun(final Description description) {
                final String testIdentifier =
                    TestCase.identifier(description.getClassName(), description.toString());

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            })
        .sortWith(
            (a, b) ->
                first.toString().equals(TestCase.identifier(a.getClassName(), a.toString()))
                    ? 1
                    : -1);
  }

  public boolean verify() {
    final JUnitCore junit = new JUnitCore();
    final JUnitResultsCollector listener = new JUnitResultsCollector();

    junit.addListener(listener);
    final boolean success = junit.run(request).wasSuccessful();

    listener.print(System.out);

    return success;
  }
}
