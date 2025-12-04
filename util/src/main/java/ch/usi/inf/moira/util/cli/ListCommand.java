package ch.usi.inf.moira.util.cli;

import ch.usi.inf.moira.util.TestsFinder;
import java.io.File;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "list",
    description = "List all test methods within a directory of Java classes without running them.",
    usageHelpAutoWidth = true)
public class ListCommand implements Runnable {
  @ParentCommand private MoiraUtil parent;

  @Parameters(
      paramLabel = "<directory>",
      description = "The direcotry containing the Java classes.",
      arity = "1")
  private File directory;

  @Option(
      names = {"-c", "-class-only"},
      description = "Show only the names of the test classes.")
  private boolean classOnly;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Override
  public void run() {
    if (!directory.exists()) {
      System.out.println("The given path does not exist");
      return;
    } else if (!directory.isDirectory()) {
      System.out.println("The given path should be a directory containing java class files");
      return;
    }

    new TestsFinder(directory, classOnly).getTests().stream().sorted().forEach(System.out::println);
  }
}
