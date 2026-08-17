package moira.util.model;

public class SimpleTestCase extends TestCase {

  public SimpleTestCase(final String testClass, final String description) {
    super(testClass, description);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof SimpleTestCase)) return false;
    return super.equals(obj);
  }
}
