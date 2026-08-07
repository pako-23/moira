package moira.util.cli;

import java.io.File;
import java.util.concurrent.Callable;
import moira.util.execution.ForkExecutor;
import moira.util.list.TestSuiteBuilder;
import moira.util.model.TestSuite;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(
    name = "list",
    description = "List all the test cases within a test suite.",
    usageHelpAutoWidth = true)
public class ListCommand implements Callable<Integer> {
  @ParentCommand private MoiraUtil parent;
  @Spec private CommandSpec spec;

  @Parameters(
      paramLabel = "<testsuite>",
      description = "The path to a file containing the test suite.",
      arity = "1")
  private File file;

  @Option(
      names = {"--app-cp"},
      description = "The application's classpath.")
  private String classpath;

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Override
  public Integer call() {
    final TestSuite suite =
        TestSuiteBuilder.builder()
            .withExecutor(new ForkExecutor(classpath))
            .withTestClassesFile(file)
            .build();

    for (int i = 0; i < suite.numberOfTestCases(); ++i)
      spec.commandLine().getOut().println(suite.getTestCase(i));

    return 0;
  }
}
