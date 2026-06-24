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

public class TargetPairsProfilerTest {

  private static final String FIELD_A = "fieldA";
  private static final String FIELD_B = "fieldB";
  private static final String TEST_A = "TestA";
  private static final String TEST_B = "TestB";

  @BeforeEach
  public void setup() {
    TargetPairsProfiler.setup();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "target-pairs-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      TargetPairsProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
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
    assertThat(makeDump("setup-no-tests").size(), is(0));
  }

  @Test
  public void testEnterExitNoAccess() {
    runIntoVirtualTest(TEST_A, () -> {});

    assertThat(makeDump("enter-exit-no-access").size(), is(0));
  }

  @Test
  public void testAccessOutsideTest() {
    TargetPairsProfiler.writeStaticField(FIELD_A);
    TargetPairsProfiler.readStaticField(FIELD_A);

    assertThat(makeDump("outside-test-noop").size(), is(0));
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

    final List<String> lines = makeDump("write-write-conflict");
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

    final List<String> lines = makeDump("read-read-conflict");
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

    final List<String> lines = makeDump("write-read-conflict");
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

    final List<String> lines = makeDump("read-write-conflict");
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

    assertThat(makeDump("different-fields").size(), is(0));
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
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    runIntoVirtualTest(
        TEST_B,
        () -> {
          TargetPairsProfiler.writeStaticField(FIELD_A);
        });

    final List<String> lines = makeDump("dump-content");
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

    final List<String> lines = makeDump("noop-methods");
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

    List<String> lines = makeDump("repeated-access");
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

    final List<String> lines = makeDump("suspend-no-conflict");
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

    final List<String> lines = makeDump("disabled-no-conflict");
    assertThat(lines.size(), is(0));
  }
}
