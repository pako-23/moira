package ch.usi.inf.moira.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class PairVerifier {
  private final Request orderedRequest;
  private final Request invertedOrderRequest;

  public static class TestMethod {
    private final Class<?> testClass;
    private final String description;

    public TestMethod(final String identifier) {
      final int beginDescription = identifier.indexOf('[');
      final String className = identifier.substring(0, beginDescription);
      if (className.isEmpty()) {
        throw new IllegalArgumentException(
            "tests should have the form <class-name>[<test-description>]");
      }

      try {
        testClass = Class.forName(className);
      } catch (final ClassNotFoundException e) {
        throw new IllegalArgumentException("failed to find testclass for test: " + identifier);
      }

      description = identifier.substring(beginDescription + 1, identifier.length() - 1);
      if (description.isEmpty())
        throw new IllegalArgumentException("missing description from test: " + identifier);
    }

    @Override
    public String toString() {
      return String.format("%s[%s]", testClass.getName(), description);
    }

    public Class<?> getTestClass() {
      return testClass;
    }
  }

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

  private String descriptionToTestID(final Description description) {
    return String.format("%s[%s]", description.getClassName(), description.toString());
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

                final String testIdentifier = descriptionToTestID(description);

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
                final String testIdentifier = descriptionToTestID(description);

                return testIdentifier.equals(first.toString())
                    || testIdentifier.equals(second.toString());
              }
            })
        .sortWith((a, b) -> first.toString().equals(descriptionToTestID(a)) ? 1 : -1);
  }

  private boolean execute(final Request request) {
    final List<Description> schedule = new ArrayList<>(2);
    final List<Boolean> outcomes = new ArrayList<>(2);

    final JUnitCore junit = new JUnitCore();
    junit.addListener(
        new RunListener() {
          @Override
          public void testStarted(final Description description) {
            schedule.add(description);
            outcomes.add(true);
          }

          @Override
          public void testFailure(final Failure failure) {
            outcomes.set(outcomes.size() - 1, false);
          }
        });
    final boolean success = junit.run(request).wasSuccessful();

    System.out.println("Running schedule:");
    for (int i = 0; i < schedule.size(); ++i) {
      final String outcome = outcomes.get(i) ? "PASS" : "FAIL";
      System.out.println("  " + descriptionToTestID(schedule.get(i)) + " -> " + outcome);
    }

    return success;
  }

  public boolean verify() {
    return execute(orderedRequest) ^ execute(invertedOrderRequest);
  }
}
