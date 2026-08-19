package moira.util.cli;

import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    description = "Detect dependencies between tests.",
    name = "detect",
    usageHelpAutoWidth = true)
public class DetectCommand implements Callable<Integer> {

  @Parameters(
      description = "The file containing the testsuite or the list of test pairs.",
      paramLabel = "<source>")
  private File file;

  @Option(
      description = "The application's classpath.",
      defaultValue = "",
      names = {"--app-cp"})
  private String classpath;

  @Option(
      names = {"-m", "--mode"},
      paramLabel = "<mode>",
      description =
          "Dependency detection algorithm. Valid values are: packed, class-only, intra-class, inter-class, target-pairs, moira (default: packed).")
  private String mode;

  @Option(
      description = "Display this help and exit.",
      names = {"-h", "--help"},
      usageHelp = true)
  private boolean help;

  @Override
  public Integer call() {
    return 0;
  }
}
