package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class HashMapTest {

  @Test
  public void createMapDefaults() {
    MapBuilder<String, Integer> builder = MapBuilder.builder();
    assertEquals(16, builder.getInitialCapacity());
    assertEquals(MapBuilder.ReferenceStrength.STRONG, builder.getKeyReferenceStrength());
    assertTrue(builder.getEquivalence().test("A", "A"));
    assertFalse(builder.getEquivalence().test("A", "B"));
    assertEquals("test".hashCode(), builder.getHashFunction().compute("test"));
  }

  @Test
  public void testInitialCapacitySetting() {
    MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.initialCapacity(32);
    assertEquals(32, builder.getInitialCapacity());
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(15));
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(0));
  }

  @Test
  public void testReferenceStrengthSetting() {
    MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.weakKeys();
    assertEquals(MapBuilder.ReferenceStrength.WEAK, builder.getKeyReferenceStrength());

    builder.strongKeys();
    assertEquals(MapBuilder.ReferenceStrength.STRONG, builder.getKeyReferenceStrength());
  }

  @Test
  public void testCustomHashFunctionAndEquivalence() {
    HashFunction<String> customHash = key -> 42;
    Equivalence<String> customEquivalence = (first, second) -> first.equalsIgnoreCase(second);

    MapBuilder<String, Integer> builder =
        MapBuilder.<String, Integer>builder()
            .hashFunction(customHash)
            .equivalence(customEquivalence);

    assertEquals(42, builder.getHashFunction().compute("anything"));
    assertTrue(builder.getEquivalence().test("a", "A"));
    assertFalse(builder.getEquivalence().test("a", "B"));
  }

  @Test
  public void testBasicPutAndGetOrPutExisting() {
    Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();
    AtomicInteger insertions = new AtomicInteger(0);
    String result =
        map.getOrPut(
            "key1",
            () -> {
              insertions.incrementAndGet();
              return "Value1";
            });

    assertEquals("Value1", result);
    assertEquals(1, insertions.get());
    assertEquals(1, map.size());
    assertEquals(4, map.capacity());

    result =
        map.getOrPut(
            "key1",
            () -> {
              insertions.incrementAndGet();
              return "NewValue";
            });

    assertEquals("Value1", result);
    assertEquals(1, insertions.get());
    assertEquals(1, map.size());
  }

  @Test
  public void testMultiplePutsAndSize() {
    Map<Integer, String> map = MapBuilder.<Integer, String>builder().initialCapacity(8).build();

    for (int i = 0; i < 3; i++) {
      int key = i + 1;
      map.getOrPut(key, () -> "Val" + key);
    }

    assertEquals(3, map.size());
    assertEquals(8, map.capacity());
    assertEquals("Val2", map.getOrPut(2, () -> "ShouldNotBeCalled"));
  }

  @Test
  public void testRehashing() {
    Map<Integer, Integer> map = MapBuilder.<Integer, Integer>builder().initialCapacity(4).build();

    map.getOrPut(1, () -> 100);
    assertEquals(1, map.size());
    assertEquals(4, map.capacity());

    map.getOrPut(2, () -> 200);
    assertEquals(2, map.size());
    assertEquals(4, map.capacity());

    map.getOrPut(3, () -> 300);
    map.getOrPut(4, () -> 400);
    assertEquals(4, map.size());
    assertEquals(8, map.capacity());

    assertEquals(100, map.getOrPut(1, () -> 0));
    assertEquals(400, map.getOrPut(4, () -> 0));
  }

  @Test
  public void testCustomEquivalence() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .initialCapacity(4)
            .equivalence((first, second) -> first.equalsIgnoreCase(second))
            .build();
    AtomicInteger insertions = new AtomicInteger(0);
    String result =
        map.getOrPut(
            "Apple",
            () -> {
              insertions.incrementAndGet();
              return "Red Fruit";
            });

    assertEquals(1, map.size());
    assertEquals(1, insertions.get());
    assertEquals("Red Fruit", result);

    result =
        map.getOrPut(
            "apple",
            () -> {
              insertions.incrementAndGet();
              return "ShouldNotBeCalled";
            });

    assertEquals("Red Fruit", result);
    assertEquals(1, map.size());
    assertEquals(1, insertions.get());
  }

  @Test
  public void testCollisionHandling() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .initialCapacity(4)
            .hashFunction((String key) -> Integer.parseInt(key))
            .equivalence(
                (String first, String second) ->
                    Integer.parseInt(first) == Integer.parseInt(second))
            .build();

    for (int i = 1; i <= 3; ++i) {
      final String value = "value" + i;
      assertEquals("value" + i, map.getOrPut(Integer.toString(i), () -> value));
      assertEquals(i, map.size());
    }
    assertEquals(8, map.capacity());

    for (int i = 1; i <= 3; ++i)
      assertEquals("value" + i, map.getOrPut(Integer.toString(i), () -> "error"));

    assertEquals("value9", map.getOrPut("9", () -> "value9"));
    assertEquals(4, map.size());
    assertEquals("value9", map.getOrPut("9", () -> "invalid"));
    assertEquals(4, map.size());
  }

  @Test
  public void testWeakKeys() {
    AtomicInteger invocations = new AtomicInteger(0);
    Map<Object, String> map =
        MapBuilder.<Object, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .keyDeletionCallback(
                (value) -> {
                  assertEquals("first-value", value);
                  invocations.incrementAndGet();
                })
            .initialCapacity(4)
            .build();

    Object key = new Object();
    WeakReference<Object> weakKey = new WeakReference<>(key);
    String result = map.getOrPut(key, () -> "first-value");
    assertEquals("first-value", result);
    assertEquals(1, map.size());
    key = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    Object newKey = new Object();
    result = map.getOrPut(newKey, () -> "second-value");

    assertEquals("second-value", result);
    assertEquals(1, map.size());
    assertEquals(1, invocations.get());
  }

  @Test
  public void testWeakKeysMultipleCollisions() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(8)
            .build();

    String key1 = new String("first-key");
    String result = map.getOrPut(key1, () -> "first-value");
    assertEquals("first-value", result);
    assertEquals(1, map.size());

    String key2 = new String("second-key");
    result = map.getOrPut(key2, () -> "second-value");
    assertEquals("second-value", result);
    assertEquals(2, map.size());

    String key3 = new String("third-key");
    result = map.getOrPut(key3, () -> "third-value");
    assertEquals("third-value", result);
    assertEquals(3, map.size());
  }

  @Test
  public void testWeakKeysCollisionDeletedKey() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(8)
            .build();

    String key1 = new String("first-key");
    String result = map.getOrPut(key1, () -> "first-value");
    assertEquals("first-value", result);
    assertEquals(1, map.size());

    String key2 = new String("second-key");
    result = map.getOrPut(key2, () -> "second-value");
    assertEquals("second-value", result);
    assertEquals(2, map.size());
    WeakReference<String> reference = new WeakReference<>(key2);
    key2 = null;
    while (reference.get() != null) {
      System.gc();
    }

    String key3 = new String("third-key");
    result = map.getOrPut(key3, () -> "third-value");
    assertEquals("third-value", result);
    assertEquals(2, map.size());
  }

  @Test
  public void testEmptyMapHasNoNext() {
    Map<Integer, String> map = MapBuilder.<Integer, String>builder().build();
    Map.Iterator<Integer, String> it = map.iterator();
    assertFalse(it.hasNext());
  }

  @Test
  public void testSingleEntryIteration() {
    Map<Integer, String> map = MapBuilder.<Integer, String>builder().build();
    map.getOrPut(10, () -> "Ten");

    Map.Iterator<Integer, String> it = map.iterator();
    assertTrue(it.hasNext());
    assertEquals(10, it.key());
    assertEquals("Ten", it.value());

    it.next();
    assertFalse(it.hasNext());
  }

  @Test
  public void testMultipleEntryIteration() {
    Map<Integer, String> map = MapBuilder.<Integer, String>builder().initialCapacity(4).build();
    map.getOrPut(1, () -> "A");
    map.getOrPut(2, () -> "B");
    map.getOrPut(3, () -> "C");
    map.getOrPut(4, () -> "D");

    Map.Iterator<Integer, String> it = map.iterator();
    Set<Integer> keysFound = new HashSet<>();

    while (it.hasNext()) {
      keysFound.add(it.key());
      it.next();
    }

    assertEquals(4, keysFound.size());
    assertTrue(keysFound.contains(1));
    assertTrue(keysFound.contains(2));
    assertTrue(keysFound.contains(3));
    assertTrue(keysFound.contains(4));
  }

  @Test
  public void testWeakKeyEvictionDuringIteration() {
    String key1 = "Keep me Strong";
    String key2 = new String("I will be collected");
    String key3 = "Keep me also Strong";

    Map<String, String> map =
        MapBuilder.<String, String>builder().initialCapacity(4).weakKeys().build();

    map.getOrPut(key1, () -> "Value1");
    map.getOrPut(key2, () -> "Value2");
    map.getOrPut(key3, () -> "Value3");

    WeakReference<String> weakKey = new WeakReference<>(key2);
    key2 = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    Map.Iterator<String, String> it = map.iterator();
    Set<String> valuesFound = new HashSet<>();
    Set<String> keysFound = new HashSet<>();

    while (it.hasNext()) {
      String key = it.key();
      if (key != null) keysFound.add(key);
      valuesFound.add(it.value());
      it.next();
    }

    assertEquals(2, keysFound.size());
    assertTrue(keysFound.contains(key1));
    assertTrue(keysFound.contains(key3));

    assertEquals(3, valuesFound.size());
    assertTrue(valuesFound.contains("Value1"));
    assertTrue(valuesFound.contains("Value2"));
    assertTrue(valuesFound.contains("Value3"));
  }

  @Test
  public void testContainsFound() {
    final String key = "somekey";
    Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();

    map.getOrPut(key, () -> "Value1");
    assertTrue(map.contains(key));
  }

  @Test
  public void testContainsNotFound() {
    Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();

    assertFalse(map.contains("somekey"));
  }

  @Test
  public void testContainsCollectedKey() {
    String key = new String("I will be collected");
    Map<String, String> map =
        MapBuilder.<String, String>builder().initialCapacity(4).weakKeys().build();

    map.getOrPut(key, () -> "Value1");
    WeakReference<String> weakKey = new WeakReference<>(key);
    key = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    for (int i = 0; i < 10; ++i) assertFalse(map.contains(new String("I will be collected")));
  }

  @Test
  public void testCompaction() {
    Object[] objects = null;
    Map<Object, Integer> map =
        MapBuilder.<Object, Integer>builder()
            .initialCapacity(64)
            .weakKeys()
            .equivalence((first, second) -> first == second)
            .build();

    for (int i = 0; i < 10; ++i) {
      objects = new Object[2048];
      for (int j = 0; j < objects.length; ++j) {
        objects[j] = new Object();
        map.getOrPut(objects[j], () -> 2);
      }

      WeakReference<Object> reference = new WeakReference<>(objects);
      objects = null;
      while (reference.get() != null) {
        System.gc();
      }

      assertEquals(4096, map.capacity());
    }
  }
}
