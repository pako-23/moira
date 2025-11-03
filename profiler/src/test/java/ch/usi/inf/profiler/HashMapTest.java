package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
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
  public void testWeakKeys() throws InterruptedException {
    Map<Object, String> map =
        MapBuilder.<Object, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
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
  }
}
