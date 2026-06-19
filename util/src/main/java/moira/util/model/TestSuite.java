package moira.util.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSuite {
  private final List<String> testClasses;
  private final List<TestCase> testCases;
  private final Map<String, Range> testClassToCases;

  public TestSuite(final List<TestCase> cases) throws IOException {
    testClasses = new ArrayList<>(cases.size());
    testCases = new ArrayList<>();
    testClassToCases = new HashMap<>();

    if (cases.size() == 0) return;

    testCases.add(cases.get(0));

    int min = 0;
    for (int i = 1; i < cases.size(); ++i) {
      final TestCase current = cases.get(i);
      final TestCase previous = cases.get(i - 1);

      if (!previous.getTestClass().equals(current.getTestClass())) {
        registerTestClass(min);
        min = i;
      }

      testCases.add(cases.get(i));
    }

    registerTestClass(min);
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

  public String getTestClass(final int index) {
    return testClasses.get(index);
  }

  public Range getTestClassCases(final String testClass) {
    return testClassToCases.get(testClass);
  }

  private void registerTestClass(final int min) {
    final int lastTestCaseIndex = testCases.size() - 1;
    final TestCase lastTestCase = testCases.get(lastTestCaseIndex);
    final String className = lastTestCase.getTestClass();

    testClasses.add(className);
    testClassToCases.put(className, new Range(min, lastTestCaseIndex + 1));
  }
}
