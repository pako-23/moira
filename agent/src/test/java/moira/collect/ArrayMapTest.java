package moira.collect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArrayMapTest {

  private static final int TEST_CAPACITY = 10;
  private ArrayMap<String> map;

  @BeforeEach
  public void setup() {
    map = new ArrayMap<>(TEST_CAPACITY);
  }

  @Test
  public void testConstructorAndBasicMethods() {
    assertEquals(TEST_CAPACITY, map.capacity());
    assertEquals(TEST_CAPACITY, map.size());
  }

  @Test
  public void testGetOrPutMisssing() {
    final String newValue = "TestValue";
    final String result = map.getOrPut(5, () -> newValue);

    assertEquals(newValue, result);
  }

  @Test
  public void testGetOrPutExisting() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertEquals(initialValue, result);
    result = map.getOrPut(key, () -> "SomeDifferentValue");
    assertEquals(initialValue, result);
  }

  @Test
  public void testContainsFound() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertEquals(initialValue, result);
    assertTrue(map.contains(key));
  }

  @Test
  public void testContainsNotFound() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertEquals(initialValue, result);
    assertFalse(map.contains(key + 1));
  }

  @Test
  public void testContainsEmpty() {
    final int key = 3;

    assertFalse(map.contains(key));
  }

  @Test
  public void testIteratorEmptyMap() {
    final Map.Iterator<Integer, String> it = map.iterator();

    assertFalse(it.hasNext());
  }

  @Test
  public void testIteratorSingleItem() {
    final String value = "Start";
    map.getOrPut(0, () -> value);

    final Map.Iterator<Integer, String> it = map.iterator();

    assertTrue(it.hasNext());
    assertEquals(0, it.key());
    assertEquals(value, it.value());
    it.next();
    assertFalse(it.hasNext());
  }

  @Test
  public void testIteratorSkipInitialEmptyValues() {
    final String value = "MapValue";
    map.getOrPut(3, () -> value);

    final Map.Iterator<Integer, String> it = map.iterator();

    assertTrue(it.hasNext());
    assertEquals(3, it.key());
    assertEquals(value, it.value());
    it.next();
    assertFalse(it.hasNext());
  }

  @Test
  public void testIteratorMultipleValues() {
    map.getOrPut(0, () -> "V0");
    map.getOrPut(1, () -> "V1");
    map.getOrPut(5, () -> "V5");

    Map.Iterator<Integer, String> it = map.iterator();

    assertEquals(0, it.key());
    assertEquals("V0", it.value());

    it.next();
    assertEquals(1, it.key());
    assertEquals("V1", it.value());

    it.next();
    assertEquals(5, it.key());
    assertEquals("V5", it.value());
    it.next();

    assertFalse(it.hasNext());
  }
}
