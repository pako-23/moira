package moira.model;

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

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof Outcome)) return false;

    final Outcome other = (Outcome) obj;

    return pass == other.pass && testCase.equals(other.testCase);
  }

  @Override
  public String toString() {
    return testCase.toString() + " " + (pass ? "OK" : "FAIL");
  }
}
