package moira.util.cli;

import moira.util.PairVerifier;
import moira.util.TestMethod;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "verify",
    description = "Verifies wether a given pair of tests has a dependency.",
    usageHelpAutoWidth = true)
public class VerifyCommand implements Runnable {
  @ParentCommand private MoiraUtil parent;

  @Parameters(
      paramLabel = "<first-test>",
      description = "The first test in the pair to verify.",
      arity = "1",
      converter = TestMethodConverter.class)
  private TestMethod firstTest;

  @Parameters(
      paramLabel = "<second-test>",
      description = "The second test in the pair to verify.",
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
      try {
        return new TestMethod(value);
      } catch (final IllegalArgumentException e) {
        throw new TypeConversionException(e.getMessage());
      }
    }
  }

  @Override
  public void run() {
    if (new PairVerifier(firstTest, secondTest).verify()) {
      System.out.println("OK");
    } else {
      System.out.println("NOT OK");
    }
  }
}
