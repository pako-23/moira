package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProfilerDumpTest {
  private ProfilerDump dump;

  @BeforeEach
  public void setUp() {
    dump = new ProfilerDump();
  }

  @Test
  public void testRegisterTestAndGetters() {
    int runningTest = dump.registerTest("TestA");
    assertTrue(runningTest >= 0);
    assertEquals("TestA", dump.getTestName(runningTest));

    runningTest = dump.registerTest("TestB");
    assertTrue(runningTest >= 0);
    assertEquals("TestB", dump.getTestName(runningTest));
  }

  @Test
  public void testIteratorNoDependencies() {
    ProfilerDump.Iterator iterator = dump.iterator();

    assertFalse(iterator.hasNext());
  }

  @Test
  public void testRegisterDependency() {
    dump.registerTest("A");
    dump.registerTest("B");
    dump.registerTest("C");

    dump.registerDependency(2, 0);

    ProfilerDump.Iterator iterator = dump.iterator();

    assertTrue(iterator.hasNext());
    assertEquals(2, iterator.getDependant());
    assertEquals(0, iterator.getDependee());

    iterator.next();
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testIteratorMultipleDependencies() {
    dump.registerTest("A");
    dump.registerTest("B");
    dump.registerTest("C");

    dump.registerDependency(0, 2);
    dump.registerDependency(1, 0);
    dump.registerDependency(1, 2);

    ProfilerDump.Iterator iterator = dump.iterator();
    assertTrue(iterator.hasNext());
    assertEquals(0, iterator.getDependant());
    assertEquals(2, iterator.getDependee());

    iterator.next();
    assertTrue(iterator.hasNext());
    assertEquals(0, iterator.getDependee());
    assertEquals(1, iterator.getDependant());

    iterator.next();
    assertTrue(iterator.hasNext());
    assertEquals(2, iterator.getDependee());
    assertEquals(1, iterator.getDependant());

    iterator.next();
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testGrowDump() {
    int capacity = 200;
    for (int i = 0; i < capacity; ++i) {
      dump.registerTest("Test" + i);
    }

    for (int i = 0; i < capacity; i += 2) {
      for (int j = 0; j < i; j += 2) {
        dump.registerDependency(i, j);
      }
    }

    int runningTest = dump.registerTest("LastTest");
    assertEquals(capacity, runningTest);
    assertEquals("LastTest", dump.getTestName(capacity));

    for (int i = 0; i < capacity; i++) {
      assertEquals("Test" + i, dump.getTestName(i));
    }

    ProfilerDump.Iterator iterator = dump.iterator();
    assertTrue(iterator.hasNext());

    for (int i = 0; i < capacity; i += 2) {
      for (int j = 0; j < i; j += 2) {
        assertEquals(i, iterator.getDependant());
        assertEquals(j, iterator.getDependee());
        iterator.next();
      }
    }

    assertFalse(iterator.hasNext());
  }

  private List<String> makeDump(final String fileName) {
    List<String> lines = null;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      dump.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  @Test
  public void testNoTestsDump() {
    List<String> lines = makeDump("no-tests-dump");

    assertEquals(0, lines.size());
  }

  @Test
  public void testExistingDependencies() {
    dump.registerTest("TestA");
    dump.registerTest("TestB");
    dump.registerTest("TestC");
    dump.registerTest("TestD");
    dump.registerDependency(0, 1);
    dump.registerDependency(2, 3);

    List<String> lines = makeDump("some-dependencies-dump");
    List<String> expected =
        Arrays.asList("TestA TestB", "TestC TestD").stream().sorted().collect(Collectors.toList());

    assertEquals(2, lines.size());
    assertEquals(expected, lines);
  }
}
