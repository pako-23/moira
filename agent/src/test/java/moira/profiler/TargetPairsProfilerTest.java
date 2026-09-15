package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TargetPairsProfilerTest {

  private static final String FIELD_A = "fieldA";
  private static final String FIELD_B = "fieldB";
  private static final String TEST_A = "TestA";
  private static final String TEST_B = "TestB";

  @BeforeEach
  public void setup() {
    TargetPairsProfiler.setup();
  }

  private List<String> makeDump() {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();

    TargetPairsProfiler.dump(new PrintStream(output));

    return Stream.of(output.toString().split("\n"))
        .filter(line -> !line.isEmpty())
        .collect(Collectors.toList());
  }

  private void runIntoVirtualTest(final String testName, final Runnable operations) {
    TargetPairsProfiler.enterTestMethod(testName);
    TargetPairsProfiler.enable();
    operations.run();
    TargetPairsProfiler.disable();
    TargetPairsProfiler.exitTestMethod();
  }

  @Test
  public void testSetupNoTests() {
    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testEnterExitNoAccess() {
    runIntoVirtualTest(TEST_A, () -> {});

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testAccessOutsideTest() {
    TargetPairsProfiler.writeStaticField(FIELD_A);
    TargetPairsProfiler.readStaticField(FIELD_A);

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testWriteStaticFieldConflict() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testReadStaticFieldConflict() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.readStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.readStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testWriteThenReadSameField() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.readStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testReadThenWriteSameField() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.readStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testNoConflictDifferentFields() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_B);
        });

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testThreeTestsSameField() {
    runIntoVirtualTest(
        "TestA",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestB",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestC",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(6));
    assertThat(
        lines,
        hasItems(
            "from: TestA, to: TestB",
            "from: TestA, to: TestC",
            "from: TestB, to: TestA",
            "from: TestB, to: TestC",
            "from: TestC, to: TestA",
            "from: TestC, to: TestB"));
  }

  @Test
  public void testMultipleTestsMultipleFields() {
    runIntoVirtualTest(
        "TestA",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestB",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
          TargetPairsProfiler.writeStaticField(FIELD_B);
        });

    runIntoVirtualTest(
        "TestC",
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_B);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        hasItems(
            "from: TestA, to: TestB",
            "from: TestB, to: TestA",
            "from: TestB, to: TestC",
            "from: TestC, to: TestB"));
  }

  @Test
  public void testDumpFileContent() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testNoopMethods() {
    final String[] tests = new String[] {TEST_A, TEST_B};
    for (final String test : tests) {
      TargetPairsProfiler.enterTestMethod(test);
      TargetPairsProfiler.suspend();
      TargetPairsProfiler.resume();
      TargetPairsProfiler.enable();
      TargetPairsProfiler.disable();
      TargetPairsProfiler.writeArrayElement(new Object[1], 0);
      TargetPairsProfiler.writeObjectField(new Object(), "f");
      TargetPairsProfiler.readArrayElement(new Object[1], 0);
      TargetPairsProfiler.readObjectField(new Object(), "f");
      TargetPairsProfiler.exitTestMethod();
    }

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(0));
  }

  @Test
  public void testRepeatedAccessSameTest() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    List<String> lines = makeDump();
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testSuspendDoesNotRegisterDependencies() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.suspend();
          TargetPairsProfiler.readStaticField(FIELD_A);
          TargetPairsProfiler.resume();
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.suspend();
          TargetPairsProfiler.writeStaticField(FIELD_A);
          TargetPairsProfiler.resume();
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(0));
  }

  @Test
  public void testDisabledDoesNotRegisterDepedencies() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetPairsProfiler.disable();
          TargetPairsProfiler.readStaticField(FIELD_A);
          TargetPairsProfiler.enable();
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.disable();
          TargetPairsProfiler.writeStaticField(FIELD_A);
          TargetPairsProfiler.enable();
        });

    final List<String> lines = makeDump();
    assertThat(lines.size(), is(0));
  }
}
