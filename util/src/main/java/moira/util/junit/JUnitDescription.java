package moira.util.junit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import moira.util.model.IndexedTestCase;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.runner.Description;

public class JUnitDescription {

  private static final Pattern pattern =
      Pattern.compile("^(.*)\\[moira: ([A-Za-z0-9.]+)(?:, index: (\\d+))?\\]$");

  private JUnitDescription() {}

  public static TestCase convert(final Description description) {
    final Matcher matcher = pattern.matcher(description.getDisplayName());

    if (!matcher.find())
      return new SimpleTestCase(description.getClassName(), description.toString());

    if (matcher.group(3) == null) return new SimpleTestCase(matcher.group(2), matcher.group(1));

    return new IndexedTestCase(
        matcher.group(2), matcher.group(1), Integer.parseInt(matcher.group(3)));
  }
}
