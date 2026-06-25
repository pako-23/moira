package moira.util.model;

public class TestCase {
  private final String testClass;
  private final String description;

  public TestCase(final String identifier) {
    final int beginDescription = identifier.indexOf('[');
    final int endDescription = identifier.lastIndexOf(']');

    if (beginDescription < 0 || endDescription < 0)
      throw new IllegalArgumentException(
          "tests should have the form <class-name>[<test-description>]");

    testClass = identifier.substring(0, beginDescription);
    if (testClass.isEmpty())
      throw new IllegalArgumentException(
          "tests should have the form <class-name>[<test-description>]");

    description = identifier.substring(beginDescription + 1, endDescription);
    if (description.isEmpty())
      throw new IllegalArgumentException("missing description from test: " + identifier);
  }

  @Override
  public String toString() {
    return String.format("%s[%s]", testClass, description);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TestCase)) return false;
    final TestCase other = (TestCase) obj;
    return other.description.equals(description) && other.testClass.equals(testClass);
  }

  @Override
  public int hashCode() {
    return description.hashCode();
  }

  public String getTestClass() {
    return testClass;
  }

  public static String identifier(final String testClass, final String description) {
    return String.format("%s[%s]", testClass, description);
  }
}
