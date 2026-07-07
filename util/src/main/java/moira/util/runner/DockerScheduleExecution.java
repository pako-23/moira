package moira.util.runner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import moira.util.docker.DockerExecution;
import moira.util.docker.DockerExecutor;
import moira.util.docker.FileContainerStream;
import moira.util.docker.LineContainerStream;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public class DockerScheduleExecution implements Callable<Outcome[]> {

  private final DockerExecutor executor;
  private final TestCase[] schedule;
    private final Path logs;

  public DockerScheduleExecution(final DockerExecutor executor, final TestCase[] schedule) {
      this(executor, schedule, null);
  }

    public DockerScheduleExecution(final DockerExecutor executor, final TestCase[] schedule, final Path logs) {
    this.executor = executor;
        this.schedule = schedule;
        this.logs = logs;
  }

  @Override
  public Outcome[] call() {
    final List<Outcome> outcomes = new ArrayList<>(schedule.length);

    final DockerExecution execution = this.executor
        .execution()
        .withStdIn(createScheduleStream())
        .withStdOut(
            new LineContainerStream() {
              @Override
              protected void processLine(final CharSequence line) {
                if (!line.equals("true") && !line.equals("false")) return;

                final int index = outcomes.size() % schedule.length;
                outcomes.add(new Outcome(schedule[index], line.equals("true")));
              }
            })
        .withArguments("moira.util.runner.ChildRunner");

      if (logs != null) execution.withStdErr(new FileContainerStream(logs));

      execution.exec();

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
