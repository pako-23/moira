package moira.util.list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moira.util.model.IndexedTestCase;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

public class TestCasesListerFilter extends Filter {
  private final List<TestCase> tests;

  public TestCasesListerFilter() {
    this.tests = new ArrayList<>();
  }

  @Override
  public String describe() {
    return "list filter";
  }

  @Override
  public boolean shouldRun(final Description description) {
    final ArrayList<Description> children = description.getChildren();

    final Map<String, Integer> frequencies = new HashMap<>(children.size());
    for (final Description child : children)
      frequencies.compute(child.toString(), (key, value) -> value == null ? 1 : value + 1);

    final Map<String, Integer> indexes = new HashMap<>();
    for (final Description child : children) {
      final String textDescription = child.toString();
      final Integer frequency = frequencies.get(textDescription);
      if (frequency == 1) {
        tests.add(new SimpleTestCase(description.getClassName(), textDescription));
      } else {
        final Integer index =
            indexes.compute(textDescription, (key, value) -> value == null ? 0 : value + 1);
        tests.add(new IndexedTestCase(description.getClassName(), textDescription, index));
      }
    }

    return false;
  }

  public List<TestCase> getTestCases() {
    return tests;
  }
}
