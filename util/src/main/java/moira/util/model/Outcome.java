package moira.util.model;

public class Outcome {
  private final TestCase testCase;
  private final boolean pass;

  public Outcome(final TestCase testCase, final boolean pass) {
    this.testCase = testCase;
    this.pass = pass;
  }

  public boolean pass() {
    return pass;
  }

  public TestCase testCase() {
    return testCase;
  }
}
