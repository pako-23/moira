package moira.util.model;

public abstract class TestCase {
  private static final String invalidIdErrorMessage =
      "tests should have the form <class-name>[<test-description>] or <class-name>[<test-description>]#<index>";

  protected final String testClass;
  protected final String description;

  public TestCase(final String testClass, final String description) {
    this.testClass = testClass;
    this.description = description;
  }

  @Override
  public String toString() {
    return String.format("%s[%s]", testClass, description);
  }

  public String getTestClass() {
    return testClass;
  }

  public static TestCase fromId(final String id) {
    final int beginDescription = id.indexOf('[');
    final int endDescription = id.lastIndexOf(']');

    if (beginDescription < 0 || endDescription < 0)
      throw new IllegalArgumentException(invalidIdErrorMessage);

    final String testClass = id.substring(0, beginDescription);
    if (testClass.isEmpty()) throw new IllegalArgumentException(invalidIdErrorMessage);

    final String description = id.substring(beginDescription + 1, endDescription);
    if (description.isEmpty()) throw new IllegalArgumentException(invalidIdErrorMessage);

    final int beginIndex = endDescription + 1;
    if (beginIndex == id.length()) return new SimpleTestCase(testClass, description);

    if (id.charAt(beginIndex) != '#') throw new IllegalArgumentException(invalidIdErrorMessage);

    try {
      final int index = Integer.parseInt(id.substring(beginIndex + 1, id.length()));
      return new IndexedTestCase(testClass, description, index);
    } catch (final Exception e) {
      throw new IllegalArgumentException(invalidIdErrorMessage);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof TestCase)) return false;
    final TestCase other = (TestCase) obj;
    return description.equals(other.description) && testClass.equals(other.testClass);
  }

  @Override
  public int hashCode() {
    return description.hashCode();
  }
}
