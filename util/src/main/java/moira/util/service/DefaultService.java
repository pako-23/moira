package moira.util.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moira.util.collector.FlakyPairsCollector;
import moira.util.execution.Executor;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.schedules.ScheduleGenerator;

public class DefaultService implements Service {

  private final Executor executor;
  private Logger logger;

  public DefaultService(final Executor executor) {
    this.executor = executor;
    this.logger =
        new Logger() {
          @Override
          public void log(String line) {}
        };
  }

  @Override
  public void setAppClassPath(final String classpath) {
    executor.setClassPath(classpath);
  }

  @Override
  public void setLogger(final Logger logger) {
    this.logger = logger;
  }

  @Override
  public TestSuite discoverTestSuite(final File filename) {
    final List<TestCase> tests = new ArrayList<>();
    final InputStream input;

    try {
      input = Files.newInputStream(filename.toPath());
    } catch (final IOException e) {
      throw new RuntimeException("failed to open testsuite file", e);
    }

    executor
        .execution()
        .withStdIn(input)
        .withArguments(moira.util.service.TestCasesLister.class.getName())
        .withStdOut(line -> tests.add(TestCase.fromId(line)))
        .exec();

    return new TestSuite(tests);
  }

  @Override
  public boolean isIndependentPair(final TestCase first, final TestCase second) {
    final Outcome[] givenOrderOutcome = executeSchedule(new TestCase[] {first, second});
    if (!givenOrderOutcome[0].pass() || !givenOrderOutcome[1].pass()) return false;

    final Outcome[] invertedOrderOutcome = executeSchedule(new TestCase[] {second, first});

    return invertedOrderOutcome[0].pass() && invertedOrderOutcome[1].pass();
  }

  @Override
  public void findFlakyPairs(
      final ScheduleGenerator generator, final FlakyPairsCollector collector) {
    final int count = generator.count();

    logger.log(String.format("progress 0/%d", count));
    for (int i = 0; i < count; ++i) {
      final TestCase[] schedule = generator.generate();
      logger.log(String.format("progress %d/%d", i + 1, count));
      collector.update(executeSchedule(schedule));
    }
  }

  @Override
  public Map<TestCase, Set<TestCase>> profile(final Profiler profiler, final File filename) {
    return null;
  }

  private Outcome[] executeSchedule(final TestCase[] schedule) {
    final List<Outcome> outcomes = new ArrayList<>(schedule.length);

    executor
        .execution()
        .withStdIn(scheduleAsStream(schedule))
        .withStdOut(
            line -> {
              if (!line.equals("true") && !line.equals("false")) return;
              final int index = outcomes.size() % schedule.length;
              outcomes.add(new Outcome(schedule[index], line.equals("true")));
            })
        .withArguments(moira.util.runner.ChildRunner.class.getName())
        .exec();

    if (outcomes.size() != schedule.length)
      throw new RuntimeException(
          String.format(
              "got %d outcomes from a schedule of length %d", outcomes.size(), schedule.length));

    return outcomes.stream().toArray(Outcome[]::new);
  }

  private InputStream scheduleAsStream(final TestCase[] schedule) {
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
