package ch.usi.inf.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
    assertThat(builder.getInitialCapacity(), is(16));
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.STRONG));
    assertTrue(builder.getEquivalence().test("A", "A"));
    assertFalse(builder.getEquivalence().test("A", "B"));
    assertEquals("test".hashCode(), builder.getHashFunction().compute("test"));
  }

  @Test
  public void testInitialCapacitySetting() {
    MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.initialCapacity(32);
    assertThat(builder.getInitialCapacity(), is(32));
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(15));
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(0));
  }

  @Test
  public void testReferenceStrengthSetting() {
    MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.weakKeys();
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.WEAK));

    builder.strongKeys();
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.STRONG));
  }

  @Test
  public void testCustomHashFunctionAndEquivalence() {
    HashFunction<String> customHash = key -> 42;
    Equivalence<String> customEquivalence = (first, second) -> first.equalsIgnoreCase(second);

    MapBuilder<String, Integer> builder =
        MapBuilder.<String, Integer>builder()
            .hashFunction(customHash)
            .equivalence(customEquivalence);

    assertThat(builder.getHashFunction().compute("anything"), is(42));
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

    assertThat(result, is("Value1"));
    assertEquals(1, insertions.get());
    assertThat(map.size(), is(1));
    assertThat(map.capacity(), is(4));

    result =
        map.getOrPut(
            "key1",
            () -> {
              insertions.incrementAndGet();
              return "NewValue";
            });

    assertThat(result, is("Value1"));
    assertEquals(1, insertions.get());
    assertThat(map.size(), is(1));
  }

  @Test
  public void testMultiplePutsAndSize() {
    Map<Integer, String> map = MapBuilder.<Integer, String>builder().initialCapacity(8).build();

    for (int i = 0; i < 3; i++) {
      int key = i + 1;
      map.getOrPut(key, () -> "Val" + key);
    }

    assertThat(map.size(), is(3));
    assertThat(map.capacity(), is(8));
    assertThat(map.getOrPut(2, () -> "ShouldNotBeCalled"), is("Val2"));
  }

  @Test
  public void testRehashing() {
    Map<Integer, Integer> map =
        MapBuilder.<Integer, Integer>builder().initialCapacity(4).loadFactor(0.5f).build();

    map.getOrPut(1, () -> 100);
    assertThat(map.size(), is(1));
    assertThat(map.capacity(), is(4));

    map.getOrPut(2, () -> 200);
    assertThat(map.size(), is(2));
    assertThat(map.capacity(), is(4));

    map.getOrPut(3, () -> 300);
    map.getOrPut(4, () -> 400);
    assertThat(map.size(), is(4));
    assertThat(map.capacity(), is(8));

    assertThat(map.getOrPut(1, () -> 0), is(100));
    assertThat(map.getOrPut(4, () -> 0), is(400));
  }

  @Test
  public void testCustomEquivalence() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .initialCapacity(4)
            .hashFunction(key -> key.toLowerCase().hashCode())
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

    assertThat(map.size(), is(1));
    assertThat(insertions.get(), is(1));
    assertThat(result, is("Red Fruit"));

    result =
        map.getOrPut(
            "apple",
            () -> {
              insertions.incrementAndGet();
              return "ShouldNotBeCalled";
            });

    assertThat(result, is("Red Fruit"));
    assertThat(map.size(), is(1));
    assertThat(insertions.get(), is(1));
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
            .loadFactor(0.5f)
            .build();

    for (int i = 1; i <= 3; ++i) {
      final String value = "value" + i;
      assertThat(map.getOrPut(Integer.toString(i), () -> value), is(value));
      assertThat(map.size(), is(i));
    }
    assertThat(map.capacity(), is(8));

    for (int i = 1; i <= 3; ++i)
      assertThat(map.getOrPut(Integer.toString(i), () -> "error"), is("value" + i));

    assertThat(map.getOrPut("9", () -> "value9"), is("value9"));
    assertThat(map.size(), is(4));
    assertThat(map.getOrPut("9", () -> "invalid"), is("value9"));
    assertThat(map.size(), is(4));
  }

  @Test
  public void testWeakKeys() {
    Map<Object, String> map =
        MapBuilder.<Object, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(4)
            .build();

    Object key = new Object();
    WeakReference<Object> weakKey = new WeakReference<>(key);
    String result = map.getOrPut(key, () -> "first-value");
    assertThat(result, is("first-value"));
    assertThat(map.size(), is(1));
    key = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    Object newKey = new Object();
    result = map.getOrPut(newKey, () -> "second-value");

    assertThat(result, is("second-value"));
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
    assertThat(result, is("first-value"));
    assertThat(map.size(), is(1));

    String key2 = new String("second-key");
    result = map.getOrPut(key2, () -> "second-value");
    assertThat(result, is("second-value"));
    assertThat(map.size(), is(2));
  }

  @Test
  public void testWeakKeysCollisionDeletedKey() {
    Map<String, String> map =
        MapBuilder.<String, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(8)
            .loadFactor(0.5f)
            .build();

    String key1 = new String("first-key");
    String result = map.getOrPut(key1, () -> "first-value");
    assertThat(result, is("first-value"));
    assertThat(map.size(), is(1));

    String key2 = new String("second-key");
    result = map.getOrPut(key2, () -> "second-value");
    assertThat(result, is("second-value"));
    assertThat(map.size(), is(2));
    WeakReference<String> reference = new WeakReference<>(key2);
    key2 = null;
    while (reference.get() != null) {
      System.gc();
    }

    String key3 = new String("third-key");
    result = map.getOrPut(key3, () -> "third-value");
    assertThat(result, is("third-value"));
    assertThat(map.size(), greaterThanOrEqualTo(3));
    assertThat(map.size(), lessThanOrEqualTo(4));
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
    assertThat(it.key(), is(10));
    assertThat(it.value(), is("Ten"));

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

    assertThat(keysFound.size(), is(4));
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

    assertThat(keysFound.size(), is(2));
    assertTrue(keysFound.contains(key1));
    assertTrue(keysFound.contains(key3));

    assertThat(valuesFound.size(), is(3));
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
    AtomicInteger invocations = new AtomicInteger(0);
    Map<Object, Integer> map =
        MapBuilder.<Object, Integer>builder()
            .initialCapacity(64)
            .weakKeys()
            .keyDeletionCallback(
                (value) -> {
                  invocations.incrementAndGet();
                })
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
    }

    assertThat(invocations.get(), greaterThan(0));
  }
}
