package ch.usi.inf.runner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

public class ScheduleRunner extends Runner {
  private final List<Method> schedule;
  private final Map<Class<?>, DelegateRunner> runners;

  public ScheduleRunner(String... tests) {
    final DelegateRunnerBuilder builder = new DelegateRunnerBuilder();
    schedule = new ArrayList<>(tests.length);
    runners = new HashMap<>(tests.length);

    for (final String test : tests) {
      String[] parts = test.split("#");
      String className = parts[0];
      String methodName = parts[1];

      try {
        Class<?> clazz = Class.forName(className);
        Method method = clazz.getMethod(methodName);

        if (!runners.containsKey(clazz)) {
          DelegateRunner runner = builder.delegateRunnerForClass(clazz);
          if (runner == null) throw new RuntimeException("failed to find runner for test: " + test);
          runners.put(clazz, runner);
        }

        schedule.add(method);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("failed to find class for test: " + test);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("failed to find test method: " + test);
      }
    }
  }

  public void run(RunNotifier notifier) {
    for (final Method testCase : schedule) {
      Class<?> testClass = testCase.getDeclaringClass();

      runners.get(testClass).run(testCase, notifier);
    }
  }

  public Description getDescription() {
    Description description = Description.createSuiteDescription("schedule");

    for (final Method testCase : schedule) {
      Class<?> testClass = testCase.getDeclaringClass();
      description.addChild(
          Description.createTestDescription(testClass.getName(), testCase.getName()));
    }

    return description;
  }
}
