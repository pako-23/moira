package moira.util.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import moira.util.TestCase;
import moira.util.TestSuite;
import moira.util.runner.ScheduleRunner;
import moira.util.tuscan.SchedulesGenerator;
import moira.util.tuscan.TuscanClassOnly;
import moira.util.tuscan.TuscanIntraClass;
import moira.util.tuscan.TuscanPacked;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "tuscan",
    description = "Run the tuscan square algorithm on a given test suite.",
    usageHelpAutoWidth = true)
public class TuscanCommand implements Runnable {
  @ParentCommand private MoiraUtil parent;

  @Parameters(
      paramLabel = "<testsuite>",
      description = "The path to a file containing the test suite.",
      arity = "1")
  private File suite;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Option(
      names = {"-m", "--mode"},
      paramLabel = "<mode>",
      description =
          "Algorithm variant. Valid values: packed, class-only, intra-class, inter-class"
              + " (default: packed).",
      defaultValue = "packed",
      converter = ModeConverter.class)
  private Mode mode;

  private static class ModeConverter implements ITypeConverter<Mode> {
    @Override
    public Mode convert(final String value) throws TypeConversionException {

      switch (value) {
        case "packed":
          return Mode.PACKED;
        case "class-only":
          return Mode.CLASS_ONLY;
        case "intra-class":
          return Mode.INTRA_CLASS;
        case "inter-class":
          return Mode.INTER_CLASS;
        default:
          throw new TypeConversionException("invalid mode provided: " + value);
      }
    }
  }

  enum Mode {
    PACKED {
      @Override
      public SchedulesGenerator generator(final TestSuite testSuite) {
        return new TuscanPacked(testSuite);
      }
    },
    CLASS_ONLY {
      @Override
      public SchedulesGenerator generator(final TestSuite testSuite) {
        return new TuscanClassOnly(testSuite);
      }
    },
    INTRA_CLASS {
      @Override
      public SchedulesGenerator generator(final TestSuite testSuite) {
        return new TuscanIntraClass(testSuite);
      }
    },
    INTER_CLASS {
      @Override
      public SchedulesGenerator generator(final TestSuite testSute) {
        throw new UnsupportedOperationException("intra-class mode not yet implemented");
      }
    };

    public abstract SchedulesGenerator generator(final TestSuite testSuite);
  }

  @Override
  public void run() {
    try {
      final SchedulesGenerator generator = mode.generator(new TestSuite(suite));
      findFlakyTests(generator);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private void findFlakyTests(final SchedulesGenerator generator)
      throws ExecutionException, InterruptedException {
    final ScheduleRunner runner = new ScheduleRunner();
    final List<ScheduleRunner.Outcome[]> results = submitSuitesExecutions(generator, runner);

    final Set<TestCase> failingInIsolation = findFailingInIsolation(results);
    final Map<TestCase, TestCase> brittle = new HashMap<>();
    final Map<TestCase, TestCase> victims = new HashMap<>();

    for (final ScheduleRunner.Outcome[] outcome : results) {
      for (int i = 1; i < outcome.length; ++i) {
        final TestCase test = outcome[i].testCase();
        final TestCase previousTest = outcome[i - 1].testCase();

        if (outcome[i].pass() && failingInIsolation.contains(test)) brittle.put(test, previousTest);
        else if (!outcome[i].pass()) victims.put(test, previousTest);
      }
    }

    for (final Map.Entry<TestCase, TestCase> pair : brittle.entrySet())
      System.out.printf(
          "from: %s, to: %s, type: brittle\n",
          pair.getKey().toString(), pair.getValue().toString());

    for (final Map.Entry<TestCase, TestCase> pair : victims.entrySet())
      System.out.printf(
          "from: %s, to: %s, type: victim\n", pair.getKey().toString(), pair.getValue().toString());
  }

  private List<ScheduleRunner.Outcome[]> submitSuitesExecutions(
      final SchedulesGenerator generator, final ScheduleRunner runner)
      throws ExecutionException, InterruptedException {
    final List<ScheduleRunner.Outcome[]> results = new ArrayList<>();
    final List<Future<ScheduleRunner.Outcome[]>> jobs = new ArrayList<>();

    while (!generator.done()) {
      jobs.add(runner.submit(generator.generate()));
    }

    for (final Future<ScheduleRunner.Outcome[]> job : jobs) results.add(job.get());

    return results;
  }

  private Set<TestCase> findFailingInIsolation(final List<ScheduleRunner.Outcome[]> results) {
    final Set<TestCase> brittle = new HashSet<>();
    for (int i = 0; i < results.size(); ++i) {
      final ScheduleRunner.Outcome[] outcome = results.get(i);
      if (!outcome[0].pass()) brittle.add(outcome[0].testCase());
    }

    return brittle;
  }
}
