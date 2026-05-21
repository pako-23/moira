package moira.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class TestSuite {
  private final Map<Class<?>, List<TestCase>> testsuite;

  public TestSuite(final File suiteFile) throws IOException {
    testsuite =
        Files.lines(suiteFile.toPath())
            .map(String::trim)
            .distinct()
            .map(
                className -> {
                  try {
                    return Class.forName(className);
                  } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                        "could not find test class: " + e.getMessage());
                  }
                })
            .collect(Collectors.toMap(Function.identity(), test -> new ArrayList<TestCase>()));

    addTestClasses(testsuite.keySet().stream().toArray(Class<?>[]::new));
  }

  public int size() {
    return testsuite.values().stream().map(List::size).reduce(0, Integer::sum);
  }

  public int testClassesSize() {
    return testsuite.size();
  }

  public List<TestCase> getTestCases() {
    return testsuite.values().stream().flatMap(c -> c.stream()).collect(Collectors.toList());
  }

  public List<Class<?>> getTestClasses() {
    return testsuite.keySet().stream().collect(Collectors.toList());
  }

  public List<TestCase> getClassTestCases(final Class<?> testClass) {
    return testsuite.get(testClass);
  }

  public void addTestClasses(final Class<?>... classes) {
    final Request request =
        Request.classes(
                Stream.of(classes)
                    .distinct()
                    .filter(testClass -> !testsuite.containsKey(testClass))
                    .toArray(Class<?>[]::new))
            .filterWith(
                new Filter() {
                  @Override
                  public String describe() {
                    return "list filter";
                  }

                  @Override
                  public boolean shouldRun(final Description description) {
                    if (description.isSuite()) return true;

                    testsuite
                        .get(description.getTestClass())
                        .add(new TestCase(TestCase.descriptionToTestID(description)));

                    return false;
                  }
                });
    final JUnitCore junit = new JUnitCore();

    junit.run(request);
  }
}
