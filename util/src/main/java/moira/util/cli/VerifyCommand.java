package moira.util.cli;

import java.util.concurrent.Callable;
import moira.util.model.TestCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "verify",
    description = "Verifies whether a given test pair passes or not.",
    usageHelpAutoWidth = true)
public class VerifyCommand implements Callable<Integer> {

  @ParentCommand private MoiraUtil parent;
  @Spec private CommandSpec spec;

  @Parameters(
      converter = TestCaseConverter.class,
      description = "The first test in the pair to verify.",
      paramLabel = "<first-test>")
  private TestCase firstTest;

  @Parameters(
      converter = TestCaseConverter.class,
      description = "The second test in the pair to verify.",
      paramLabel = "<second-test>")
  private TestCase secondTest;

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

  private static class TestCaseConverter implements ITypeConverter<TestCase> {
    @Override
    public TestCase convert(final String value) throws TypeConversionException {
      try {
        return TestCase.fromId(value);
      } catch (final IllegalArgumentException e) {
        throw new TypeConversionException(e.getMessage());
      }
    }
  }

  @Override
  public Integer call() {
    final boolean isIndependent =
        parent.service().isIndependentPair(firstTest, secondTest, classpath);

    if (isIndependent) spec.commandLine().getOut().println("pair is independent");
    else spec.commandLine().getOut().println("pair is not independent");

    return 0;
  }
}
