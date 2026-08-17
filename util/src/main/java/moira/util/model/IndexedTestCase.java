package moira.util.model;

public class IndexedTestCase extends TestCase {

  private final int index;

  public IndexedTestCase(final String testClass, final String description, final int index) {
    super(testClass, description);

    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public String toString() {
    return String.format("%s[%s]#%d", testClass, description, index);
  }

  @Override
  public boolean equals(final Object obj) {
    if (!super.equals(obj)) return false;
    if (!(obj instanceof IndexedTestCase)) return false;

    final IndexedTestCase other = (IndexedTestCase) obj;
    return other.index == index;
  }

  @Override
  public int hashCode() {
    return super.hashCode() ^ index;
  }
}
