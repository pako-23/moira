package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProfilerDumpTest {
  private ProfilerDump dump;

  @BeforeEach
  public void setUp() {
    dump = new ProfilerDump();
  }

  @Test
  public void testConstructorAndInitialState() {
    assertEquals(-1, dump.getRunningTest());
  }

  @Test
  public void testRegisterTestAndGetters() {
    dump.registerTest("TestA");
    assertEquals("TestA", dump.getTestName(dump.getRunningTest()));
    dump.unregisterTest();

    dump.registerTest("TestB");
    assertEquals("TestB", dump.getTestName(dump.getRunningTest()));
  }

  @Test
  public void testUnregisterTest() {
    dump.registerTest("TestA");
    assertEquals("TestA", dump.getTestName(dump.getRunningTest()));
    dump.unregisterTest();

    assertEquals(-1, dump.getRunningTest());
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
      dump.unregisterTest();
    }

    for (int i = 0; i < capacity; i += 2) {
      for (int j = 0; j < i; j += 2) {
        dump.registerDependency(i, j);
      }
    }

    dump.registerTest("LastTest");
    assertEquals(capacity, dump.getRunningTest());
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
}
