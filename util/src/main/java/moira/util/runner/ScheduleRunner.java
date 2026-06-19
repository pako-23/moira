package moira.util.runner;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import moira.util.docker.DockerExecutor;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public class ScheduleRunner extends Thread {

  private final DockerExecutor executor;
  private final ScheduleGenerator generator;
  private Semaphore semaphore;
  private CompletionService<Outcome[]> pool;
  private int completed;
  private final int count;

  public ScheduleRunner(final ScheduleRunnerBuilder builder) {
    this.executor = builder.getDockerExecutor();
    this.generator = builder.getScheduleGenerator();
    this.semaphore = new Semaphore(builder.getConcurrencyLevel());
    this.pool =
        new ExecutorCompletionService<>(
            Executors.newFixedThreadPool(builder.getConcurrencyLevel()));
    completed = 0;
    count = generator.count();
  }

  @Override
  public void run() {
    try {
      while (!generator.done()) {
        final TestCase[] schedule = generator.generate();

        semaphore.acquire();
        pool.submit(new DockerScheduleExecution(executor, schedule));
      }

    } catch (final Exception e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }
  }

  public Outcome[] getOutcome() {
    if (completed >= count) return null;

    try {
      final Future<Outcome[]> future = pool.take();
      semaphore.release();

      ++completed;

      return future.get();
    } catch (final InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
      return null;
    }
  }

  // private Outcome[] executeSchedule(final TestCase[] schedule)
  //     throws IOException, InterruptedException {
  //   final Process runner = createChildRunner();
  //   try (BufferedWriter writer =
  //       new BufferedWriter(new OutputStreamWriter(runner.getOutputStream()))) {
  //     for (final TestCase test : schedule) {
  //       writer.write(test.toString());
  //       writer.newLine();
  //     }
  //   }

  //   final Outcome[] outcomes = new Outcome[schedule.length];
  //   try (BufferedReader reader =
  //       new BufferedReader(new InputStreamReader(runner.getInputStream()))) {
  //     for (int i = 0; i < outcomes.length; ++i) {
  //       final String line = reader.readLine();
  //       if (line == null) throw new RuntimeException("failed to read test case execution
  // result");

  //       outcomes[i] = new Outcome(schedule[i], Boolean.parseBoolean(line));
  //     }
  //   }

  //   final int code = runner.waitFor();
  //   if (code != 0) throw new RuntimeException("runner returned with non zero stauts: " + code);

  //   return outcomes;
  // }

  // private Process createChildRunner() throws IOException {
  //   return new ProcessBuilder(
  //           String.join(File.separator, System.getProperty("java.home"), "bin", "java"),
  //           "-classpath",
  //           System.getProperty("java.class.path"),
  //           "moira.util.runner.ChildRunner")
  //       .start();
  // }
}
