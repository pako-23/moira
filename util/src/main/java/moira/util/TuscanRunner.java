package moira.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class TuscanRunner {
  private final Request request;

  public TuscanRunner(final List<TestCase> testsuite) {
    final List<AbstractMap.SimpleEntry<Class<?>, Set<String>>> classes =
        new ArrayList<>(testsuite.size());

    classes.add(
        new AbstractMap.SimpleEntry<Class<?>, Set<String>>(
            testsuite.get(0).getTestClass(),
            Stream.of(testsuite.get(0).toString()).collect(Collectors.toSet())));

    for (int i = 1; i < testsuite.size(); ++i) {
      final TestCase method = testsuite.get(i);
      final AbstractMap.SimpleEntry<Class<?>, Set<String>> pair = classes.get(classes.size() - 1);

      if (method.getTestClass().equals(pair.getKey())) pair.getValue().add(method.toString());
      else
        classes.add(
            new AbstractMap.SimpleEntry<Class<?>, Set<String>>(
                method.getTestClass(), Stream.of(method.toString()).collect(Collectors.toSet())));
    }

    final Map<String, Integer> order = new HashMap<>();
    for (int i = 0; i < testsuite.size(); ++i) order.put(testsuite.get(i).toString(), i);

    request =
        Request.classes(
                classes.stream().map(AbstractMap.SimpleEntry::getKey).toArray(Class<?>[]::new))
            .filterWith(
                new Filter() {

                  private int lastIndex = 0;

                  @Override
                  public String describe() {
                    return "tuscan filter";
                  }

                  @Override
                  public boolean shouldRun(final Description description) {
                    if (lastIndex >= classes.size()) return false;
                    if (description.isSuite()) return true;

                    final String testId = TestCase.descriptionToTestID(description);
                    final Set<String> tests = classes.get(lastIndex).getValue();

                    if (!tests.contains(testId)) return false;

                    tests.remove(testId);
                    if (tests.size() == 0) ++lastIndex;

                    return true;
                  }
                })
            .sortWith(
                (a, b) -> {
                  if (a.isSuite() || b.isSuite()) return 0;

                  final int firstIndex = order.get(TestCase.descriptionToTestID(a));
                  final int secondIndex = order.get(TestCase.descriptionToTestID(b));

                  return firstIndex - secondIndex;
                });
  }

  public void run() {
    final JUnitCore junit = new JUnitCore();
    final ScheduleListener listener = new ScheduleListener(128);

    junit.addListener(listener);
    junit.run(request);

    listener.print(System.out);
  }
}
