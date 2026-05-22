package moira.util.cli;

import java.io.File;
import moira.util.TestSuite;
import moira.util.runner.ScheduleRunner;
import moira.util.tuscan.TuscanClassOnly;
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
              + " (default: class-only).",
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
    PACKED,
    CLASS_ONLY,
    INTRA_CLASS,
    INTER_CLASS
  }

  @Override
  public void run() {
    try {
      final ScheduleRunner runner = new ScheduleRunner();
      final TestSuite testSuite = new TestSuite(suite);

      switch (mode) {
        case PACKED:
          new TuscanPacked(testSuite).run(runner);
          break;
        case CLASS_ONLY:
          new TuscanClassOnly(testSuite).run(runner);
          break;
        case INTRA_CLASS:
          throw new UnsupportedOperationException("intra-class mode not yet implemented");
        case INTER_CLASS:
          throw new UnsupportedOperationException("inter-class mode not yet implemented");
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
