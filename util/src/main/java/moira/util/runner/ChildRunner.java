package moira.util.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.TestCase;

public class ChildRunner {
  private static final PrintStream NULL_PRINT_STREAM =
      new PrintStream(
          new OutputStream() {
            @Override
            public void write(final int b) {}
          });

  public static void main(String[] args) throws IOException {
    final List<TestCase> tests = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) {
        tests.add(new TestCase(line));
      }
    }

    final PrintStream originalOut = System.out;
    final PrintStream originalErr = System.err;
    System.setOut(NULL_PRINT_STREAM);
    System.setErr(NULL_PRINT_STREAM);

    final List<Boolean> results = new JUnitExecutor(tests).run();
    System.setOut(originalOut);
    System.setErr(originalErr);
    for (final boolean result : results) {
      System.out.println(result);
    }

    System.exit(0);
  }
}
