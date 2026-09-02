package moira.util.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class JUnit3Runner extends Runner implements Filterable, Sortable {

  private final Class<?> testClass;
  private ArrayList<Test> suite;
  private final Map<Test, Integer> indexedTests;

  public JUnit3Runner(final Class<?> testClass, final ArrayList<Test> suite) {
    this.testClass = testClass;
    this.suite = suite;
    this.indexedTests = new HashMap<>();

    findIndexedTests();
  }

  @Override
  public void run(final RunNotifier notifier) {
    final TestResult result = new TestResult();

    result.addListener(new TestListenerAdapter(notifier));
    for (final Test test : suite) {
      if (result.shouldStop()) break;

      test.run(result);
    }
  }

  @Override
  public Description getDescription() {

    final Description description = Description.createSuiteDescription(testClass.getName());

    for (final Test test : suite) description.addChild(getMoiraTestDescription(test));

    return description;
  }

  @Override
  public void filter(final Filter filter) throws NoTestsRemainException {
    suite.removeIf(test -> !filter.shouldRun(getMoiraTestDescription(test)));
    if (suite.isEmpty()) {
      throw new NoTestsRemainException();
    }
  }

  @Override
  public void sort(final Sorter sorter) {
    suite.sort((a, b) -> sorter.compare(getMoiraTestDescription(a), getMoiraTestDescription(b)));
  }

  private void findIndexedTests() {
    final Map<String, Integer> indices = getIndicesMapping();
    for (final Test test : suite) {
      final Description description = getTestDescription(test);
      final Integer index =
          indices.computeIfPresent(description.toString(), (key, value) -> value + 1);
      if (index != null) indexedTests.put(test, index - 1);
    }
  }

  private Map<String, Integer> getIndicesMapping() {
    final Map<String, Integer> frequencies = new HashMap<>(suite.size());
    for (final Test test : suite)
      frequencies.compute(
          getTestDescription(test).toString(), (key, value) -> value == null ? 1 : value + 1);

    return frequencies.entrySet().stream()
        .filter(entry -> entry.getValue() > 1)
        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, entry -> 0));
  }

  private Description getMoiraTestDescription(final Test test) {
    return Description.createSuiteDescription(getMoiraDisplayName(test));
  }

  private String getMoiraDisplayName(final Test test) {
    final Description description = getTestDescription(test);
    final Integer index = indexedTests.get(test);

    if (index == null)
      return String.format("%s[moira: %s]", description.getDisplayName(), testClass.getName());
    else
      return String.format(
          "%s[moira: %s, index: %d]", description.getDisplayName(), testClass.getName(), index);
  }

  private Description getTestDescription(final Test test) {
    if (test instanceof Describable) {
      final Describable facade = (Describable) test;
      return facade.getDescription();
    }
    return Description.createTestDescription(getTestEffectiveClass(test), getTestName(test));
  }

  private Class<? extends Test> getTestEffectiveClass(final Test test) {
    return test.getClass();
  }

  private String getTestName(final Test test) {
    if (test instanceof TestCase) {
      return ((TestCase) test).getName();
    } else {
      return test.toString();
    }
  }

  private final class TestListenerAdapter implements TestListener {
    private final RunNotifier notifier;

    private TestListenerAdapter(final RunNotifier notifier) {
      this.notifier = notifier;
    }

    @Override
    public void startTest(final Test test) {
      notifier.fireTestStarted(getMoiraTestDescription(test));
    }

    @Override
    public void endTest(final Test test) {
      notifier.fireTestFinished(getMoiraTestDescription(test));
    }

    @Override
    public void addError(final Test test, final Throwable e) {
      final Failure failure = new Failure(getMoiraTestDescription(test), e);
      notifier.fireTestFailure(failure);
    }

    @Override
    public void addFailure(final Test test, final AssertionFailedError e) {
      addError(test, e);
    }
  }
}
