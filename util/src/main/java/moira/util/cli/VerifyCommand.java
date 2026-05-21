package moira.util.cli;

import moira.util.PairVerifier;
import moira.util.TestCase;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "verify",
    description = "Verifies that a given pair of tests passes or not.",
    usageHelpAutoWidth = true)
public class VerifyCommand implements Runnable {
  @ParentCommand private MoiraUtil parent;

  @Parameters(
      paramLabel = "<first-test>",
      description = "The first test in the pair to verify.",
      arity = "1",
      converter = TestCaseConverter.class)
  private TestCase firstTest;

  @Parameters(
      paramLabel = "<second-test>",
      description = "The second test in the pair to verify.",
      arity = "1",
      converter = TestCaseConverter.class)
  private TestCase secondTest;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  private static class TestCaseConverter implements ITypeConverter<TestCase> {
    @Override
    public TestCase convert(final String value) throws TypeConversionException {
      try {
        return new TestCase(value);
      } catch (final IllegalArgumentException e) {
        throw new TypeConversionException(e.getMessage());
      }
    }
  }

  @Override
  public void run() {
    final long start = System.currentTimeMillis();
    final boolean passed = new PairVerifier(firstTest, secondTest).verify();
    final long end = System.currentTimeMillis();
    if (passed) {
      System.out.println("OK");
    } else {
      System.out.println("NOT OK");
    }
    System.out.println("Time: " + (end - start) / 1000f);
  }
}
