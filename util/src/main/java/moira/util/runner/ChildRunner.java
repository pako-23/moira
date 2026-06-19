package moira.util.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import moira.util.model.TestCase;

public class ChildRunner {
  public static void main(String[] args) throws IOException {
    final List<TestCase> tests = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.equals("__END__")) break;
        tests.add(new TestCase(line));
      }
    }

    final PrintStream originalOut = System.out;
    System.setOut(System.err);

    final List<Boolean> results = new JUnitExecutor(tests).run();
    System.setOut(originalOut);

    for (final boolean result : results) {
      System.out.println(result);
    }

    System.exit(0);
  }
}
