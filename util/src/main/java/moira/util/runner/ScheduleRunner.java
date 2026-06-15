package moira.util.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import moira.util.TestCase;

public class ScheduleRunner extends Thread {
  private final ScheduleGenerator generator;
  private final BlockingQueue<Outcome[]> outcomes;
  private volatile boolean finished;

  public ScheduleRunner(final ScheduleGenerator generator) {
    this.generator = generator;
    outcomes = new LinkedBlockingQueue<>();
    finished = false;
  }

  @Override
  public void run() {
    try {
      while (generator.done()) {
        final TestCase[] schedule = generator.generate();
        outcomes.put(executeSchedule(schedule));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }

    finished = true;
  }

  public Outcome[] getOutcome() {
    try {
      if (finished) return null;
      return outcomes.take();
    } catch (final InterruptedException e) {
      finished = true;
      return null;
    }
  }

  private Outcome[] executeSchedule(final TestCase[] schedule)
      throws IOException, InterruptedException {
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
        if (line == null) throw new RuntimeException("failed to read test case execution result");

        outcomes[i] = new Outcome(schedule[i], Boolean.parseBoolean(line));
      }
    }

    final int code = runner.waitFor();
    if (code != 0) throw new RuntimeException("runner returned with non zero stauts: " + code);

    return outcomes;
  }

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
}
