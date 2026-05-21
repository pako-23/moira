package moira.util.cli;

import java.io.File;
import java.io.IOException;
import moira.util.TestSuite;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "list",
    description = "List all the test cases within a test suite.",
    usageHelpAutoWidth = true)
public class ListCommand implements Runnable {
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
      new TestSuite(suite).getTestCases().stream().forEach(System.out::println);
    } catch (final IOException e) {
      System.err.println("could not read file: " + e.getMessage());
      System.exit(1);
    }
  }
}
