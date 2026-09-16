package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

public class VerifyTest extends AcceptanceTest {

  @Test
  public void testIndependentPair() {
    execute(
        "verify",
        testId(com.example.SimplePassingTest.class, "testPass1"),
        testId(com.example.OtherPassingTest.class, "testPass1"));

    assertThat(outputLines(), contains("pair is independent"));
  }

  @Test
  public void testDependentPair() {
    execute(
        "verify",
        testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
        testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"));

    assertThat(outputLines(), contains("pair is not independent"));
  }
}
