package ch.usi.inf.runner.cli;

import ch.usi.inf.runner.ScheduleRunner;
import ch.usi.inf.runner.ScheduleRunner.TestMethod;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "run",
    description = "Run a test pair in the given order.",
    usageHelpAutoWidth = true)
public class RunCommand implements Runnable {
  @ParentCommand private JUnitLauncher parent;

  @Parameters(
      paramLabel = "<first-test>",
      description = "The first test in the pair to run.",
      arity = "1",
      converter = TestMethodConverter.class)
  private TestMethod firstTest;

  @Parameters(
      paramLabel = "<second-test>",
      description = "The second test in the pair to run.",
      arity = "1",
      converter = TestMethodConverter.class)
  private TestMethod secondTest;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  private static class TestMethodConverter implements ITypeConverter<TestMethod> {
    @Override
    public TestMethod convert(final String value) throws TypeConversionException {
      final String[] parts = value.split("#");
      if (parts.length != 2)
        throw new TypeConversionException("Tests should have the form <class-name>#<method-name>");

      try {
        return new TestMethod(parts[0], parts[1]);
      } catch (final Exception e) {
        throw new TypeConversionException(e.getMessage());
      }
    }
  }

  @Override
  public void run() {
    new ScheduleRunner(firstTest, secondTest).execute();
  }
}
