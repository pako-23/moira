package moira.util.runner;

import com.sun.jna.Library;
import com.sun.jna.Native;
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
  private static final boolean isStderrTerminal = CLibrary.INSTANCE.isatty(2) != 0;

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
      printProgress();
      for (int i = 0; i < count; ++i) {
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
      printProgress();

      return future.get();
    } catch (final InterruptedException | ExecutionException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
      return null;
    }
  }

  private void printProgress() {
    if (isStderrTerminal) System.err.printf("progress %d/%d\r", completed, count);
    else System.err.printf("progress %d/%d\n", completed, count);
  }

  private interface CLibrary extends Library {
    final CLibrary INSTANCE = Native.load("c", CLibrary.class);

    int isatty(int fd);
  }
}
