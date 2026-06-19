package moira.util.cli;

import java.io.File;
import moira.util.docker.DockerExecutor;
import moira.util.list.TestSuiteBuilder;
import moira.util.model.TestSuite;
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
  private File file;

  @Option(
      names = {"-app-cp"},
      description = "The application's classpath.")
  private String classpath;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Override
  public void run() {

    final TestSuite suite =
        TestSuiteBuilder.builder()
            .withDockerExecutor(new DockerExecutor(classpath))
            .withTestClassesFile(file)
            .build();

    for (int i = 0; i < suite.numberOfTestCases(); ++i) System.out.println(suite.getTestCase(i));
  }
}
