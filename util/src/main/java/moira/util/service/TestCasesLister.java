package moira.util.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.junit.JUnitRunner;
import moira.util.model.TestCase;
import moira.util.runner.TestRunner;

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

  private static String[] readTestClasses(final InputStream input) throws IOException {
    final List<String> classes = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;

      while ((line = reader.readLine()) != null) classes.add(line);
    }

    return classes.stream().toArray(String[]::new);
  }

  private static List<TestCase> detectTestCases(final String... classes) {
    final TestRunner runner = new JUnitRunner();
    final List<TestCase> tests = new ArrayList<>();

    if (classes.length == 0) return tests;

    runner
        .request(classes)
        .withFilter(
            test -> {
              tests.add(test);
              return true;
            });

    return tests;
  }

  private static void outputTestCases(final OutputStream output, final List<TestCase> testCases) {
    final PrintStream stream = new PrintStream(output);

    for (final TestCase testCase : testCases) stream.println(testCase);
  }
}
