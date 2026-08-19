package moira.util.cli;

import java.io.File;
import java.util.concurrent.Callable;
import moira.util.factory.MoiraFactory;
import moira.util.model.TestSuite;
import moira.util.service.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(
    name = "list",
    description = "List all the test cases within a testsuite.",
    usageHelpAutoWidth = true)
public class ListCommand implements Callable<Integer> {
  @ParentCommand private MoiraUtil parent;
  @Spec private CommandSpec spec;

  @Parameters(
      paramLabel = "<testsuite>",
      description = "The path to a file containing the testsuite.")
  private File file;

  @Option(
      description = "The application's classpath.",
      defaultValue = "",
      names = {"--app-cp"})
  private String classpath;

  @Option(
      names = {"-h", "--help"},
      description = "Display help and exit.",
      usageHelp = true)
  private boolean help;

  @Override
  public Integer call() {
    final MoiraFactory factory = parent.factory();
    final Service service = factory.createService();

    if (!classpath.isEmpty()) service.setAppClassPath(classpath);
    final TestSuite testsuite = service.discoverTestSuite(file);
    for (int i = 0; i < testsuite.numberOfTestCases(); ++i)
      spec.commandLine().getOut().println(testsuite.getTestCase(i).toString());

    return 0;
  }
}
