package moira.util.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import moira.util.TestCase;

public class ScheduleRunner {
  private static final ExecutorService executor = Executors.newFixedThreadPool(1);

  private Process createChildRunner() throws IOException {
    return new ProcessBuilder(
            String.join(File.separator, System.getProperty("java.home"), "bin", "java"),
            "-classpath",
            System.getProperty("java.class.path"),
            "moira.util.runner.ChildRunner")
        .start();
  }

  public static class Outcome {
    private final TestCase testCase;
    private final boolean pass;

    private Outcome(final TestCase testCase, final boolean pass) {
      this.testCase = testCase;
      this.pass = pass;
    }

    public boolean pass() {
      return pass;
    }

    public TestCase testCase() {
      return testCase;
    }
  }

  public Future<Outcome[]> submit(final TestCase[] schedule) {
    return executor.submit(
        new Callable<Outcome[]>() {
          @Override
          public Outcome[] call() throws IOException, InterruptedException {
            final Process runner = createChildRunner();
            try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(runner.getOutputStream()))) {
              for (final TestCase test : schedule) {
                writer.write(test.toString());
                writer.newLine();
              }
            }

            final Outcome[] outcomes = new Outcome[schedule.length];
            try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(runner.getInputStream()))) {
              for (int i = 0; i < outcomes.length; ++i) {
                final String line = reader.readLine();
                if (line == null)
                  throw new RuntimeException("failed to read test case execution result");

                outcomes[i] = new Outcome(schedule[i], Boolean.parseBoolean(line));
              }
            }

            final int code = runner.waitFor();
            if (code != 0)
              throw new RuntimeException("runner returned with non zero stauts: " + code);

            return outcomes;
          }
        });
  }
}
