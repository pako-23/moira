package moira.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import moira.collector.FlakyPairsCollector;
import moira.execution.Executor;
import moira.model.Outcome;
import moira.model.TestCase;
import moira.model.TestSuite;
import moira.schedules.ScheduleGenerator;

public class DefaultService implements Service {
  private static final String FROM_PREFIX = "from: ";
  private static final String TO_SEPARATOR = ", to: ";

  private final Executor executor;
  private Logger logger;

  public DefaultService(final Executor executor) {
    this.executor = executor;
    this.logger =
        new Logger() {
          @Override
          public void log(final String line) {}
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

    executor
        .execution()
        .withStdIn(openTestSuiteFile(filename))
        .withArguments(moira.service.TestCasesLister.class.getName())
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
  public Map<TestCase, Set<TestCase>> profile(final ProfileOptions options) {
    final InputStream tests = openTestSuiteFile(options.getTestSuite());
    final String agent = Agent.path();
    final Map<TestCase, Set<TestCase>> dependencies = new HashMap<>();
    final List<String> args = new ArrayList<>();

    args.add("-Xss2m");

    if (JavaVersion.version() >= 9) {
      args.add("--add-opens");
      args.add("java.base/java.util=ALL-UNNAMED");
      args.add("--add-opens");
      args.add("java.base/sun.security.jca=ALL-UNNAMED");
    }

    args.add("-javaagent:" + agent);
    args.add("-Xbootclasspath/a:" + agent);
    args.add("-Dmoira.profiler.name=" + options.getProfiler().getProfilerClass());

    if (options.getFilter() != null) args.add("-Dmoira.agent.filter=" + options.getFilter());
    if (options.getSuspend() != null) args.add("-Dmoira.agent.suspend=" + options.getSuspend());

    args.add(moira.service.AgentRunner.class.getName());

    executor
        .execution()
        .withStdIn(tests)
        .withStdOut(
            line -> {
              final String dependency = line.trim();
              if (!dependency.startsWith(FROM_PREFIX)) return;

              final int separator = dependency.indexOf(TO_SEPARATOR, FROM_PREFIX.length());
              if (separator < 0 || separator != dependency.lastIndexOf(TO_SEPARATOR)) return;

              final String fromId = dependency.substring(FROM_PREFIX.length(), separator);
              final String toId = dependency.substring(separator + TO_SEPARATOR.length());

              final TestCase from;
              final TestCase to;
              try {
                from = TestCase.fromId(fromId);
                to = TestCase.fromId(toId);
              } catch (final IllegalArgumentException ignored) {
                return;
              }
              if (!from.toString().equals(fromId) || !to.toString().equals(toId)) return;

              dependencies.compute(
                  from,
                  (key, value) -> {
                    if (value == null) value = new HashSet<>();
                    value.add(to);
                    return value;
                  });
            })
        .withArguments(args.stream().toArray(String[]::new))
        .exec();

    return dependencies;
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
        .withArguments(moira.service.ScheduleExecutor.class.getName())
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

  private InputStream openTestSuiteFile(final File filename) {
    try {
      return Files.newInputStream(filename.toPath());
    } catch (final IOException e) {
      throw new RuntimeException("failed to open testsuite file", e);
    }
  }
}
