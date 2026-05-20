package moira.util.cli;

import java.io.File;
import java.io.IOException;
import moira.util.TestSuiteLister;
import moira.util.TuscanRunner;
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
      new TuscanRunner(new TestSuiteLister(suite).getTestMethods()).run();
    } catch (final IOException e) {
      System.err.println("could not read file: " + e.getMessage());
      System.exit(1);
    }
  }
}
