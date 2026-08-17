package moira.util.list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.model.TestCase;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

public class TestCasesLister {

  private TestCasesLister() {}

  public static void main(final String[] args) throws IOException {
    final PrintStream stdout = System.out;

    System.setOut(System.err);
    run(System.in, stdout);
  }

  public static void run(final InputStream input, final OutputStream output) throws IOException {
    final Request request = readTestClasses(input);
    final List<TestCase> testCases = detectTestCases(request);

    outputTestCases(output, testCases);
  }

  private static Request readTestClasses(final InputStream input) throws IOException {
    final List<String> classes = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;

      while ((line = reader.readLine()) != null) classes.add(line);
    }

    return requestFromClassesList(classes);
  }

  private static List<TestCase> detectTestCases(final Request request) {
    final TestCasesListerFilter filter = new TestCasesListerFilter();
    final JUnitCore junit = new JUnitCore();
    junit.run(request.filterWith(filter));
    return filter.getTestCases();
  }

  private static void outputTestCases(final OutputStream output, final List<TestCase> testCases) {
    final PrintStream stream = new PrintStream(output);

    for (final TestCase testCase : testCases) stream.println(testCase);
  }

  private static Request requestFromClassesList(final List<String> classes) {
    return Request.classes(
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
            .toArray(Class<?>[]::new));
  }
}
