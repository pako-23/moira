package moira.util.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.junit.ScheduleRun;
import moira.util.model.TestCase;

public class TestCasesLister {

  private TestCasesLister() {}

  public static void main(final String[] args) throws IOException {
    final PrintStream stdout = System.out;

    System.setOut(System.err);
    run(System.in, stdout);
  }

  public static void run(final InputStream input, final OutputStream output) throws IOException {
    final List<TestCase> testCases = detectTestCases(readTestClasses(input));

    outputTestCases(output, testCases);
  }

  private static Class<?>[] readTestClasses(final InputStream input) throws IOException {
    final List<String> classes = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;

      while ((line = reader.readLine()) != null) classes.add(line);
    }

    return classes.stream()
        .map(
            className -> {
              try {
                return Class.forName(className);
              } catch (final ClassNotFoundException e) {
                return null;
              }
            })
        .filter(clazz -> clazz != null)
        .toArray(Class<?>[]::new);
  }

  private static List<TestCase> detectTestCases(final Class<?>... classes) {
    final TestCasesListerFilter filter = new TestCasesListerFilter();

    new ScheduleRun(classes).withFilter(filter).run();

    return filter.getTestCases();
  }

  private static void outputTestCases(final OutputStream output, final List<TestCase> testCases) {
    final PrintStream stream = new PrintStream(output);

    for (final TestCase testCase : testCases) stream.println(testCase);
  }
}
