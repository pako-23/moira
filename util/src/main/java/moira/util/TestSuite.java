package moira.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class TestSuite {
  private final List<Class<?>> testClasses;
  private final List<TestCase> testCases;
  private final Map<Class<?>, Range> testClassToCases;

  public TestSuite(final File testSuiteFile) throws IOException {
    testClasses = new ArrayList<>();
    testCases = new ArrayList<>();
    testClassToCases = new HashMap<>();

    Files.lines(testSuiteFile.toPath())
        .map(String::trim)
        .distinct()
        .map(
            className -> {
              try {
                return Class.forName(className);
              } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException("could not find test class: " + e.getMessage());
              }
            })
        .forEach(testClass -> this.registerTestClass(testClass));
  }

  public int numberOfTestCases() {
    return testCases.size();
  }

  public TestCase getTestCase(final int index) {
    return testCases.get(index);
  }

  public int numberOfTestClasses() {
    return testClasses.size();
  }

  public Class<?> getTestClass(final int index) {
    return testClasses.get(index);
  }

  public Range getTestClassCases(final Class<?> testClass) {
    return testClassToCases.get(testClass);
  }

  private void registerTestClass(final Class<?> testClass) {
    final List<TestCase> cases = findTestCases(testClass);
    testClasses.add(testClass);
    testClassToCases.put(testClass, new Range(testCases.size(), testCases.size() + cases.size()));
    testCases.addAll(cases);
  }

  public void addTestClasses(final Class<?>... classes) {
    Stream.of(classes)
        .filter(testClass -> !testClassToCases.containsKey(testClass))
        .forEach(testClass -> this.registerTestClass(testClass));
  }

  private List<TestCase> findTestCases(final Class<?> testClass) {
    final List<TestCase> testClassCases = new ArrayList<>();
    final Request request =
        Request.aClass(testClass)
            .filterWith(
                new Filter() {
                  @Override
                  public String describe() {
                    return "list filter";
                  }

                  @Override
                  public boolean shouldRun(final Description description) {
                    if (description.isSuite()) return true;

                    testClassCases.add(new TestCase(TestCase.descriptionToTestID(description)));

                    return false;
                  }
                });

    final JUnitCore junit = new JUnitCore();

    junit.run(request);
    ;

    return testClassCases;
  }
}
