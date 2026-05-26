package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TargetedPairsProfilerTest {

  private static final String FIELD_A = "fieldA";
  private static final String FIELD_B = "fieldB";
  private static final String TEST_A = "TestA";
  private static final String TEST_B = "TestB";

  @BeforeEach
  public void setup() {
    TargetedPairsProfiler.setup();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "targeted-pairs-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      TargetedPairsProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  private void runIntoVirtualTest(final String testName, final Runnable operations) {
    TargetedPairsProfiler.enterTestMethod(testName);
    TargetedPairsProfiler.enable();
    operations.run();
    TargetedPairsProfiler.disable();
    TargetedPairsProfiler.exitTestMethod();
  }

  @Test
  public void testSetupNoTests() {
    assertThat(makeDump("setup-no-tests").size(), is(0));
  }

  @Test
  public void testEnterExitNoAccess() {
    runIntoVirtualTest(TEST_A, () -> {});

    assertThat(makeDump("enter-exit-no-access").size(), is(0));
  }

  @Test
  public void testAccessOutsideTest() {
    TargetedPairsProfiler.writeStaticField(FIELD_A);
    TargetedPairsProfiler.readStaticField(FIELD_A);

    assertThat(makeDump("outside-test-noop").size(), is(0));
  }

  @Test
  public void testWriteStaticFieldConflict() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("write-write-conflict");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testReadStaticFieldConflict() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.readStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.readStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("read-read-conflict");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testWriteThenReadSameField() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.readStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("write-read-conflict");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testReadThenWriteSameField() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.readStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("read-write-conflict");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testNoConflictDifferentFields() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_B);
        });

    assertThat(makeDump("different-fields").size(), is(0));
  }

  @Test
  public void testThreeTestsSameField() {
    runIntoVirtualTest(
        "TestA",
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestB",
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestC",
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("three-tests");
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
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        "TestB",
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
          TargetedPairsProfiler.writeStaticField(FIELD_B);
        });

    runIntoVirtualTest(
        "TestC",
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_B);
        });

    final List<String> lines = makeDump("multi-field");
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
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("dump-content");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testNoopMethods() {
    final String[] tests = new String[] {TEST_A, TEST_B};
    for (final String test : tests) {
      TargetedPairsProfiler.enterTestMethod(test);
      TargetedPairsProfiler.suspend();
      TargetedPairsProfiler.resume();
      TargetedPairsProfiler.enable();
      TargetedPairsProfiler.disable();
      TargetedPairsProfiler.writeArrayElement(new Object[1], 0);
      TargetedPairsProfiler.writeObjectField(new Object(), "f");
      TargetedPairsProfiler.readArrayElement(new Object[1], 0);
      TargetedPairsProfiler.readObjectField(new Object(), "f");
      TargetedPairsProfiler.exitTestMethod();
    }

    final List<String> lines = makeDump("noop-methods");
    assertThat(lines.size(), is(0));
  }

  @Test
  public void testRepeatedAccessSameTest() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.writeStaticField(FIELD_A);
        });

    List<String> lines = makeDump("repeated-access");
    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestB, to: TestA"));
  }

  @Test
  public void testSuspendDoesNotRegisterDependencies() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.suspend();
          TargetedPairsProfiler.readStaticField(FIELD_A);
          TargetedPairsProfiler.resume();
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.suspend();
          TargetedPairsProfiler.writeStaticField(FIELD_A);
          TargetedPairsProfiler.resume();
        });

    final List<String> lines = makeDump("suspend-no-conflict");
    assertThat(lines.size(), is(0));
  }

  @Test
  public void testDisabledDoesNotRegisterDepedencies() {
    runIntoVirtualTest(
        TEST_A,
        () -> {
          TargetedPairsProfiler.disable();
          TargetedPairsProfiler.readStaticField(FIELD_A);
          TargetedPairsProfiler.enable();
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetedPairsProfiler.disable();
          TargetedPairsProfiler.writeStaticField(FIELD_A);
          TargetedPairsProfiler.enable();
        });

    final List<String> lines = makeDump("disabled-no-conflict");
    assertThat(lines.size(), is(0));
  }
}
