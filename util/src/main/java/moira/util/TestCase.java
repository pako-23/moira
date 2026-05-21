package moira.util;

import org.junit.runner.Description;

public class TestCase {
  private final Class<?> testClass;
  private final String description;

  public TestCase(final String identifier) {
    final int beginDescription = identifier.indexOf('[');
    final int endDescription = identifier.lastIndexOf(']');

    if (beginDescription < 0 || endDescription < 0) {
      throw new IllegalArgumentException(
          "tests should have the form <class-name>[<test-description>]");
    }

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

    description = identifier.substring(beginDescription + 1, endDescription);
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

  public static String descriptionToTestID(final Description description) {
    return String.format("%s[%s]", description.getClassName(), description.toString());
  }
}
