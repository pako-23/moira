package moira.util.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;

public class OutcomeTest {
  private static final TestCase SAMPLE_CASE = new TestCase("com.example.Foo[bar]");

  @Test
  public void testPassingOutcome() {
    final Outcome outcome = new Outcome(SAMPLE_CASE, true);
    assertThat(outcome.pass(), is(true));
    assertThat(outcome.testCase(), is(sameInstance(SAMPLE_CASE)));
  }

  @Test
  public void testFailingOutcome() {
    final Outcome outcome = new Outcome(SAMPLE_CASE, false);
    assertThat(outcome.pass(), is(false));
    assertThat(outcome.testCase(), is(sameInstance(SAMPLE_CASE)));
  }
}
