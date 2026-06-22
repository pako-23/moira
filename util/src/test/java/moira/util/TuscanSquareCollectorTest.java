package moira.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TuscanSquareCollectorTest {

  private static final TestCase TEST_A = new TestCase("A[a]");
  private static final TestCase TEST_B = new TestCase("B[b]");
  private static final TestCase TEST_C = new TestCase("C[c]");
  private static final TestCase TEST_D = new TestCase("D[d]");
  private FlakyPairsCollector collector;

  @BeforeEach
  private void setup() {
    collector = new TuscanSquareCollector();
  }

  private String capturePrint() {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintStream stream = new PrintStream(buffer)) {
      collector.print(stream);
    }
    return buffer.toString();
  }

  @Test
  public void testSingleOutcomeOnlyIsolation() {
    collector.update(
        new Outcome[] {
          new Outcome(TEST_A, true),
          new Outcome(TEST_B, true),
          new Outcome(TEST_C, true),
          new Outcome(TEST_D, true),
        });
    assertThat(capturePrint(), is(emptyString()));
  }

  @Test
  public void testNoBrittleWhenIsolationPasses() {
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_B, true), new Outcome(TEST_A, true)});
    assertThat(capturePrint(), is(emptyString()));
  }

  @Test
  public void testBrittleSetterWithinSameUpdate() {
    collector.update(
        new Outcome[] {
          new Outcome(TEST_A, false), new Outcome(TEST_B, true), new Outcome(TEST_C, true)
        });
    collector.update(
        new Outcome[] {
          new Outcome(TEST_B, true), new Outcome(TEST_A, true), new Outcome(TEST_C, true)
        });
    assertThat(capturePrint(), containsString("from: B[b], to: A[a], type: brittle"));
  }

  @Test
  public void testVictimPolluterPair() {
    collector.update(new Outcome[] {new Outcome(TEST_B, true), new Outcome(TEST_A, true)});
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, false)});
    assertThat(capturePrint(), containsString("from: A[a], to: B[b], type: victim"));
  }

  @Test
  public void testMultipleBrittleSetters() {
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_C, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false), new Outcome(TEST_A, true)});
    final String output = capturePrint();
    assertThat(output, containsString("from: A[a], to: B[b], type: brittle"));
    assertThat(output, containsString("from: C[c], to: B[b], type: brittle"));
  }

  @Test
  public void testMixedVictimPolluterAndBrittleAcrossUpdates() {
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_C, true), new Outcome(TEST_D, false)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false), new Outcome(TEST_A, true)});
    final String output = capturePrint();
    assertThat(output, containsString("from: A[a], to: B[b], type: brittle"));
    assertThat(output, containsString("from: C[c], to: D[d], type: victim"));
  }
}
