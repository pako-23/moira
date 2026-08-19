package moira.util.cli;

import java.io.File;
import java.util.concurrent.Callable;
import moira.util.FlakyPairsCollector;
import moira.util.factory.DetectionMode;
import moira.util.factory.MoiraFactory;
import moira.util.runner.ScheduleGenerator;
import moira.util.service.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(
    description = "Detect dependencies between tests.",
    name = "detect",
    usageHelpAutoWidth = true)
public class DetectCommand implements Callable<Integer> {
  @ParentCommand private MoiraUtil parent;
  @Spec private CommandSpec spec;

  @Parameters(
      description = "The file containing the testsuite or the list of test pairs.",
      paramLabel = "<source>")
  private File source;

  @Option(
      description = "The application's classpath.",
      defaultValue = "",
      names = {"--app-cp"})
  private String classpath;

  @Option(
      converter = DetectionModeConverter.class,
      description =
          "Dependency detection algorithm. Valid values are: tuscan-packed, tuscan-class-only, tuscan-intra-class, tuscan-inter-class, target-pairs, moira (default: tuscan-packed).",
      defaultValue = "tuscan-packed",
      names = {"-m", "--mode"},
      paramLabel = "<mode>")
  private DetectionMode mode;

  @Option(
      description = "Display help and exit.",
      names = {"-h", "--help"},
      usageHelp = true)
  private boolean help;

  private static class DetectionModeConverter implements ITypeConverter<DetectionMode> {
    @Override
    public DetectionMode convert(final String value) throws Exception {
      final DetectionMode mode = DetectionMode.fromString(value);

      if (mode == null) throw new TypeConversionException("invalid mode provided: " + value);
      return mode;
    }
  }

  @Override
  public Integer call() {
    final MoiraFactory factory = parent.factory();
    final Service service = factory.createService();

    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, source);
    final FlakyPairsCollector collector = factory.createFlakyPairsCollector(mode, source);

    if (!classpath.isEmpty()) service.setAppClassPath(classpath);

    service.findFlakyPairs(generator, collector);
    collector.print(spec.commandLine().getOut());

    return 0;
  }
}
