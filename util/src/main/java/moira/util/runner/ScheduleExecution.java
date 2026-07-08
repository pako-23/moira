package moira.util.runner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import moira.util.execution.Executor;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public class ScheduleExecution implements Callable<Outcome[]> {

  private final Executor executor;
  private final TestCase[] schedule;

  public ScheduleExecution(final Executor executor, final TestCase[] schedule) {
    this.executor = executor;
    this.schedule = schedule;
  }

  @Override
  public Outcome[] call() {
    final List<Outcome> outcomes = new ArrayList<>(schedule.length);

    executor
        .execution()
        .withStdIn(createScheduleStream())
        .withStdOut(
            line -> {
              if (!line.equals("true") && !line.equals("false")) return;
              final int index = outcomes.size() % schedule.length;
              outcomes.add(new Outcome(schedule[index], line.equals("true")));
            })
        .withArguments("moira.util.runner.ChildRunner")
        .exec();

    if (outcomes.size() != schedule.length)
      throw new RuntimeException(
          String.format(
              "got %d outcomes from a schedule of length %d", outcomes.size(), schedule.length));

    return outcomes.stream().toArray(Outcome[]::new);
  }

  private InputStream createScheduleStream() {
    return new InputStream() {
      private byte[] line = null;
      private int currentLine;
      private int currentByte;

      @Override
      public int read() throws IOException {
        if (currentLine >= schedule.length) return -1;

        if (line == null) {
          line = schedule[currentLine].toString().getBytes();
          currentByte = 0;
        }

        if (currentByte == line.length) {
          ++currentLine;
          line = null;
          return '\n';
        }

        return line[currentByte++];
      }
    };
  }
}
