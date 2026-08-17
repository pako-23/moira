package moira.util.runner;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.InitializationError;

public class JUnitExecutor {
  private final Request request;

  public JUnitExecutor(final List<TestCase> testsuite) {
    final List<AbstractMap.SimpleEntry<String, Set<String>>> testClasses =
        new ArrayList<>(testsuite.size());

    testClasses.add(
        new AbstractMap.SimpleEntry<String, Set<String>>(
            testsuite.get(0).getTestClass(),
            Stream.of(testsuite.get(0).toString()).collect(Collectors.toSet())));

    for (int i = 1; i < testsuite.size(); ++i) {
      final TestCase method = testsuite.get(i);
      final AbstractMap.SimpleEntry<String, Set<String>> pair =
          testClasses.get(testClasses.size() - 1);

      if (method.getTestClass().equals(pair.getKey())) pair.getValue().add(method.toString());
      else
        testClasses.add(
            new AbstractMap.SimpleEntry<String, Set<String>>(
                method.getTestClass(), Stream.of(method.toString()).collect(Collectors.toSet())));
    }

    final Map<String, Integer> order = new HashMap<>();
    for (int i = 0; i < testsuite.size(); ++i) order.put(testsuite.get(i).toString(), i);

    request =
        classes(
                testClasses.stream()
                    .map(AbstractMap.SimpleEntry::getKey)
                    .map(
                        className -> {
                          try {
                            return Class.forName(className);
                          } catch (final ClassNotFoundException e) {

                            return null;
                          }
                        })
                    .filter(clazz -> clazz != null)
                    .toArray(Class<?>[]::new))
            .filterWith(
                new Filter() {

                  private int lastIndex = 0;

                  @Override
                  public String describe() {
                    return "executor filter";
                  }

                  @Override
                  public boolean shouldRun(final Description description) {
                    if (lastIndex >= testClasses.size()) return false;
                    if (description.isSuite()) return true;

                    final String testId =
                        new SimpleTestCase(description.getClassName(), description.toString())
                            .toString();
                    final Set<String> tests = testClasses.get(lastIndex).getValue();

                    if (!order.containsKey(testId)) {
                      System.out.println(testId);
                      return true;
                    }

                    if (!tests.contains(testId)) return false;

                    tests.remove(testId);
                    if (tests.size() == 0) ++lastIndex;

                    return false;
                  }
                })
            .sortWith(
                (a, b) -> {
                  if (a.isSuite() || b.isSuite()) return 0;

                  final int firstIndex =
                      order.get(new SimpleTestCase(a.getClassName(), a.toString()).toString());
                  final int secondIndex =
                      order.get(new SimpleTestCase(b.getClassName(), b.toString()).toString());

                  return firstIndex - secondIndex;
                });
  }

  private static Request classes(final Class<?>... classes) {
    try {
      final AllDefaultPossibilitiesBuilder builder = new AllDefaultPossibilitiesBuilder();
      final Computer computer = new Computer();
      final Runner suite = computer.getSuite(builder, classes);
      return runner(suite);
    } catch (final InitializationError e) {
      return runner(new ErrorReportingRunner(e, classes));
    }
  }

  private static Request runner(final Runner runner) {
    return new Request() {
      @Override
      public Runner getRunner() {
        return runner;
      }
    };
  }

  public List<Boolean> run() {
    final JUnitCore junit = new JUnitCore();
    final JUnitResultsCollector listener = new JUnitResultsCollector();

    junit.addListener(listener);
    junit.run(request);

    return listener.getOutcomes();
  }
}
