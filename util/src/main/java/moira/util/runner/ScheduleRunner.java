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
            System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
            "-classpath",
            System.getProperty("java.class.path"),
            "moira.util.runner.ChildRunner")
        .start();
  }

  public Future<boolean[]> submit(final TestCase[] schedule) {
    return executor.submit(
        new Callable<boolean[]>() {
          @Override
          public boolean[] call() throws IOException, InterruptedException {
            final Process runner = createChildRunner();
            try (BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(runner.getOutputStream()))) {
              for (final TestCase test : schedule) {
                writer.write(test.toString());
                writer.newLine();
              }
            }

            final boolean[] outcomes = new boolean[schedule.length];
            try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(runner.getInputStream()))) {
              for (int i = 0; i < outcomes.length; ++i) {
                final String line = reader.readLine();
                if (line == null)
                  throw new RuntimeException("failed to read test case execution result");

                outcomes[i] = Boolean.parseBoolean(line);
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
