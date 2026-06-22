package moira.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import org.junit.jupiter.api.Test;

public class PairsCollectorTest {

  private static final TestCase TEST_A = new TestCase("A[a]");
  private static final TestCase TEST_B = new TestCase("B[b]");
  private static final TestCase TEST_C = new TestCase("C[c]");
  private static final TestCase TEST_D = new TestCase("D[d]");

  private static Map<TestCase, Set<TestCase>> pairsMap(final TestCase... pairs) {
    final Map<TestCase, Set<TestCase>> result = new HashMap<>();

    for (int i = 1; i < pairs.length; i += 2) {
      final TestCase from = pairs[i - 1];
      final TestCase to = pairs[i];

      result.computeIfAbsent(from, key -> new HashSet<>()).add(to);
      ;
    }

    return result;
  }

  private static String capturePrint(final PairsCollector collector) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintStream stream = new PrintStream(buffer)) {
      collector.print(stream);
    }
    return buffer.toString();
  }

  @Test
  public void testEmptyPairsMap() {
    final PairsCollector collector = new PairsCollector(new HashMap<>());
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, false)});
    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testPassingPair() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testPassingPairWithOtherOutcome() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B));
    collector.update(
        new Outcome[] {
          new Outcome(TEST_A, true), new Outcome(TEST_B, true), new Outcome(TEST_C, true)
        });
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_C, true)});
    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testVictimPolluterPair() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, false)});
    assertThat(capturePrint(collector), containsString("from: A[a], to: B[b], type: victim"));
  }

  @Test
  public void testVictimPolluterPairNonRegistedPair() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_C));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, false)});
    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testBrittleWhenPredecessorNotScheduled() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false)});
    assertThat(capturePrint(collector), containsString("from: A[a], to: B[b], type: brittle"));
  }

  @Test
  public void testBrittleNonRegisterdPair() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_C));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false)});
    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testMultipleVictimPolluterPairs() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B, TEST_B, TEST_C));
    collector.update(
        new Outcome[] {
          new Outcome(TEST_A, true), new Outcome(TEST_B, false), new Outcome(TEST_C, false)
        });
    final String output = capturePrint(collector);
    assertThat(output, containsString("from: A[a], to: B[b], type: victim"));
    assertThat(output, containsString("from: B[b], to: C[c], type: victim"));
  }

  @Test
  public void testBrittleWithMultipleSetters() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B, TEST_C, TEST_B));
    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_C, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false)});

    final String output = capturePrint(collector);
    assertThat(output, containsString("from: A[a], to: B[b], type: brittle"));
    assertThat(output, containsString("from: C[c], to: B[b], type: brittle"));
  }

  @Test
  public void testVictimPolluterAndBrittleMixed() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B, TEST_C, TEST_B));

    collector.update(new Outcome[] {new Outcome(TEST_A, true), new Outcome(TEST_B, true)});
    collector.update(new Outcome[] {new Outcome(TEST_C, true), new Outcome(TEST_B, false)});
    collector.update(new Outcome[] {new Outcome(TEST_B, false)});

    final String output = capturePrint(collector);
    assertThat(output, containsString("from: A[a], to: B[b], type: brittle"));
    assertThat(output, containsString("from: C[c], to: B[b], type: victim"));
  }

  @Test
  public void testNoFlakyBehaviorAllPass() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B, TEST_C, TEST_D));
    collector.update(
        new Outcome[] {
          new Outcome(TEST_A, true),
          new Outcome(TEST_B, true),
          new Outcome(TEST_C, true),
          new Outcome(TEST_D, true)
        });
    collector.update(new Outcome[] {new Outcome(TEST_B, true), new Outcome(TEST_D, true)});

    assertThat(capturePrint(collector), is(emptyString()));
  }

  @Test
  public void testOnlyOutcomeInIsolation() {
    final PairsCollector collector = new PairsCollector(pairsMap(TEST_A, TEST_B));
    collector.update(new Outcome[] {new Outcome(TEST_A, true)});
    assertThat(capturePrint(collector), is(emptyString()));
  }
}
