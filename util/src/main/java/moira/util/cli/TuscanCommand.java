package moira.util.cli;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import moira.util.FlakyPairsCollector;
import moira.util.PairsCollector;
import moira.util.TuscanSquareCollector;
import moira.util.docker.DockerExecutor;
import moira.util.list.TestSuiteBuilder;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleGenerator;
import moira.util.runner.ScheduleRunner;
import moira.util.runner.ScheduleRunnerBuilder;
import moira.util.tuscan.PairCover;
import moira.util.tuscan.TargetPairsGenerator;
import moira.util.tuscan.TuscanClassOnly;
import moira.util.tuscan.TuscanInterClass;
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
      paramLabel = "<file>",
      description = "The path to a file containing the test suite or the list of test pairs.",
      arity = "1")
  private File file;

  @Option(
      names = {"-app-cp"},
      description = "The application's classpath.")
  private String classpath;

  @Option(
      names = {"-p", "-parallelism"},
      description = "The maximum number of parallel threads.")
  private int parallelism = ScheduleRunnerBuilder.DEFAULT_CONCURRENCY_LEVEL;

  @Option(
      names = {"-m", "-mode"},
      paramLabel = "<mode>",
      description =
          "Algorithm variant. Valid values: packed, class-only, intra-class, inter-class, targeted-pairs, pair-cover"
              + " (default: packed).",
      defaultValue = "packed",
      converter = TuscanCommandModeConverter.class)
  private TuscanCommandMode mode;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  private static class TuscanCommandModeConverter implements ITypeConverter<TuscanCommandMode> {
    @Override
    public TuscanCommandMode convert(final String value) throws TypeConversionException {

      switch (value) {
        case "packed":
          return TuscanCommandMode.PACKED;
        case "class-only":
          return TuscanCommandMode.CLASS_ONLY;
        case "intra-class":
          return TuscanCommandMode.INTRA_CLASS;
        case "inter-class":
          return TuscanCommandMode.INTER_CLASS;
        case "targeted-pairs":
          return TuscanCommandMode.TARGETED_PAIRS;
        case "pair-cover":
          return TuscanCommandMode.PAIR_COVER;
        default:
          throw new TypeConversionException("invalid mode provided: " + value);
      }
    }
  }

  enum TuscanCommandMode {
    PACKED,
    CLASS_ONLY,
    INTRA_CLASS,
    INTER_CLASS,
    TARGETED_PAIRS,
    PAIR_COVER;
  }

  @Override
  public void run() {
    final DockerExecutor executor = new DockerExecutor(classpath);

    final ScheduleRunner runner =
        ScheduleRunnerBuilder.builder()
            .withDockerExecutor(executor)
            .withConcurrencyLevel(parallelism)
            .withScheduleGenerator(constructScheduleGenerator(executor))
            .build();
    final FlakyPairsCollector collector = constructFlakyTestsCollector();

    try {
      runner.start();

      Outcome[] outcome;
      while ((outcome = runner.getOutcome()) != null) collector.update(outcome);

      runner.join();
      collector.print();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private ScheduleGenerator constructScheduleGenerator(final DockerExecutor executor) {
    switch (mode) {
      case PACKED:
        return new TuscanPacked(
            TestSuiteBuilder.builder()
                .withDockerExecutor(executor)
                .withTestClassesFile(file)
                .build());
      case CLASS_ONLY:
        return new TuscanClassOnly(
            TestSuiteBuilder.builder()
                .withDockerExecutor(executor)
                .withTestClassesFile(file)
                .build());
      case INTRA_CLASS:
        return new TuscanIntraClass(
            TestSuiteBuilder.builder()
                .withDockerExecutor(executor)
                .withTestClassesFile(file)
                .build());
      case INTER_CLASS:
        return new TuscanInterClass(
            TestSuiteBuilder.builder()
                .withDockerExecutor(executor)
                .withTestClassesFile(file)
                .build());
      case TARGETED_PAIRS:
        return new TargetPairsGenerator(parsePairs(file));
      case PAIR_COVER:
        return new PairCover(parsePairs(file));
      default:
        throw new RuntimeException("invalid mode provided");
    }
  }

  private FlakyPairsCollector constructFlakyTestsCollector() {
    switch (mode) {
      case PACKED:
      case CLASS_ONLY:
      case INTRA_CLASS:
      case INTER_CLASS:
        return new TuscanSquareCollector();
      case TARGETED_PAIRS:
      case PAIR_COVER:
        return new PairsCollector(parsePairs(file));
      default:
        throw new RuntimeException("invalid mode provided");
    }
  }

  // private void findFlakyTests(final ScheduleGenerator generator)
  //     throws InterruptedException, IOException {
  //   final ScheduleRunner runner = new ScheduleRunner(generator);
  //   final FlakyPairsCollector collector = mode.collector(file);

  //   runner.run();

  //   Outcome[] outcome = runner.getOutcome();
  //   while (outcome != null) {
  //     collector.update(outcome);
  //     outcome = runner.getOutcome();
  //   }

  //   collector.print();
  //   runner.join();
  // }

  private static Map<TestCase, Set<TestCase>> parsePairs(final File input) {
    final Map<TestCase, Set<TestCase>> pairs = new HashMap<>();
    try (final Scanner scanner = new Scanner(input)) {
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine().trim();
        if (line.isEmpty()) continue;

        final String[] parts = line.split(", to:");
        if (parts.length != 2) continue;

        final String from = parts[0].substring("from: ".length());
        final String to = parts[1].substring("to: ".length());
        pairs.computeIfAbsent(new TestCase(from), key -> new HashSet<>()).add(new TestCase(to));
      }
    } catch (final IOException e) {
      throw new RuntimeException("failed to read pair files: " + e.getMessage());
    }

    return pairs;
  }
}
