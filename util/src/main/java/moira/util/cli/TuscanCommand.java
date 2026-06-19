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
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.runner.ScheduleGenerator;
import moira.util.runner.ScheduleRunner;
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
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Option(
      names = {"-m", "--mode"},
      paramLabel = "<mode>",
      description =
          "Algorithm variant. Valid values: packed, class-only, intra-class, inter-class, targeted-pairs, pair-cover"
              + " (default: packed).",
      defaultValue = "packed",
      converter = TuscanCommandModeConverter.class)
  private TuscanCommandMode mode;

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
    PACKED {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new TuscanPacked(new TestSuite(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new TuscanSquareCollector();
      }
    },
    CLASS_ONLY {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new TuscanClassOnly(new TestSuite(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new TuscanSquareCollector();
      }
    },
    INTRA_CLASS {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new TuscanIntraClass(new TestSuite(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new TuscanSquareCollector();
      }
    },
    INTER_CLASS {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new TuscanInterClass(new TestSuite(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new TuscanSquareCollector();
      }
    },
    TARGETED_PAIRS {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new TargetPairsGenerator(parsePairs(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new PairsCollector(parsePairs(input));
      }
    },
    PAIR_COVER {
      @Override
      public ScheduleGenerator generator(final File input) throws IOException {
        return new PairCover(parsePairs(input));
      }

      @Override
      public FlakyPairsCollector collector(final File input) throws IOException {
        return new PairsCollector(parsePairs(input));
      }
    };

    public abstract ScheduleGenerator generator(final File input) throws IOException;

    public abstract FlakyPairsCollector collector(final File input) throws IOException;
  }

  @Override
  public void run() {
    try {
      final ScheduleGenerator generator = mode.generator(file);
      findFlakyTests(generator);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  private void findFlakyTests(final ScheduleGenerator generator)
      throws InterruptedException, IOException {
    final ScheduleRunner runner = new ScheduleRunner(generator);
    final FlakyPairsCollector collector = mode.collector(file);

    runner.run();

    Outcome[] outcome = runner.getOutcome();
    while (outcome != null) {
      collector.update(outcome);
      outcome = runner.getOutcome();
    }

    collector.print();
    runner.join();
  }

  private static Map<TestCase, Set<TestCase>> parsePairs(final File input) throws IOException {
    final Map<TestCase, Set<TestCase>> pairs = new HashMap<>();
    try (Scanner scanner = new Scanner(input)) {
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine().trim();
        if (line.isEmpty()) continue;

        final String[] parts = line.split(", to:");
        if (parts.length != 2) continue;

        final String from = parts[0].substring("from: ".length());
        final String to = parts[1].substring("to: ".length());
        pairs.computeIfAbsent(new TestCase(from), key -> new HashSet<>()).add(new TestCase(to));
      }
    }

    return pairs;
  }
}
