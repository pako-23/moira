package moira.util.cli;

import java.io.File;
import moira.util.TestSuite;
import moira.util.runner.ScheduleRunner;
import moira.util.tuscan.TuscanClassOnly;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

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

  @Override
  public void run() {
    try {
      final ScheduleRunner runner = new ScheduleRunner();
      new TuscanClassOnly(new TestSuite(suite)).run(runner);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
