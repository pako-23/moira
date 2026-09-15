package moira.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OutcomeTest {
  private static final TestCase SAMPLE_CASE = TestCase.fromId("com.example.Foo[bar]");

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

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testEqualsDifferentClass(final boolean passing) {
    final Outcome outcome = new Outcome(SAMPLE_CASE, passing);

    assertThat(outcome.equals((Object) "Hello"), is(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testEqualsDifferentTestCase(final boolean passing) {
    final Outcome outcome = new Outcome(SAMPLE_CASE, passing);
    final Outcome other = new Outcome(TestCase.fromId("com.example.OtherTest[desc]"), passing);

    assertThat(outcome.equals(other), is(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testEqualsDifferentPassingResult(final boolean passing) {
    final Outcome outcome = new Outcome(SAMPLE_CASE, passing);
    final Outcome other = new Outcome(SAMPLE_CASE, !passing);

    assertThat(outcome.equals(other), is(false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  public void testEqualsSameOutcome(final boolean passing) {
    final String testId = "com.example.ExampleTest[desc]";
    final Outcome outcome = new Outcome(TestCase.fromId(testId), passing);
    final Outcome other = new Outcome(TestCase.fromId(testId), passing);

    assertThat(outcome.equals(other), is(true));
  }
}
