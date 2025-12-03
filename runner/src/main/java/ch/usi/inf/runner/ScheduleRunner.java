package ch.usi.inf.runner;

import java.lang.reflect.Method;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class ScheduleRunner {
  private final Request request;

  public static class TestMethod {
    private final Class<?> testClass;
    private final Method method;

    public TestMethod(final String testClass, final String method) {
      if (testClass.isEmpty()) throw new IllegalArgumentException("Class name cannot be empty");
      if (method.isEmpty()) throw new IllegalArgumentException("Method name cannot be empty");

      try {
        this.testClass = Class.forName(testClass);
        this.method = this.testClass.getMethod(method);
      } catch (final ClassNotFoundException e) {
        throw new RuntimeException("failed to find class for test class: " + testClass);
      } catch (final NoSuchMethodException e) {
        throw new RuntimeException(
            String.format("failed to find method '%s' for class '%s'", method, testClass));
      }
    }

    @Override
    public String toString() {
      return String.format("%s(%s)", method.getName(), testClass.getName());
    }

    public Class<?> getTestClass() {
      return testClass;
    }

    public Method getMethod() {
      return method;
    }
  }

  public ScheduleRunner(final TestMethod first, final TestMethod second) {
    if (first.getTestClass().equals(second.getTestClass())) {
      request = singleClassRunner(first, second);
    } else {
      request = multipleClassesRunner(first, second);
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

                return description.toString().equals(first.toString())
                    || description.toString().equals(second.toString());
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
                return description.toString().equals(first.toString())
                    || description.toString().equals(second.toString());
              }
            })
        .sortWith((a, b) -> first.toString().equals(a.toString()) ? 1 : -1);
  }

  public void execute() {
    final JUnitCore junit = new JUnitCore();
    junit.addListener(new TextListener(new RealSystem()));
    junit.run(request);
  }
}
