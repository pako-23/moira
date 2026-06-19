package moira.util.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import moira.util.model.TestCase;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class TestCasesLister {
  public static void main(final String[] args) throws IOException {
    final List<String> classes = readTestClasses();
    final List<String> cases = new ArrayList<>();

    final Request request =
        Request.classes(
                classes.stream()
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
                  @Override
                  public String describe() {
                    return "list filter";
                  }

                  @Override
                  public boolean shouldRun(Description description) {
                    if (description.isSuite()) return true;

                    cases.add(
                        TestCase.identifier(description.getClassName(), description.toString()));

                    return false;
                  }
                });

    final JUnitCore junit = new JUnitCore();

    junit.run(request);

    cases.stream().forEach(System.out::println);
  }

  private static List<String> readTestClasses() throws IOException {
    final List<String> classes = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.equals("__END__")) break;
        classes.add(line);
      }
    }

    return classes;
  }
}
