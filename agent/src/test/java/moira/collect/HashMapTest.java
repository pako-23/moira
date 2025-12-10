package moira.collect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HashMapTest {

  @Test
  public void createMapDefaults() {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    assertThat(builder.getInitialCapacity(), is(16));
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.STRONG));
    assertThat(builder.getHashFunction().compute("test"), is("test".hashCode()));
  }

  @Test
  public void testStrongReferenceStrengthEquivalence() {
    Equivalence<String> equivalence = MapBuilder.ReferenceStrength.STRONG.defaultEquivalence();
    assertThat(equivalence.test("A", "B"), is(false));
    assertThat(equivalence.test("A", "A"), is(true));
  }

  @Test
  public void testWeakReferenceStrengthEquivalence() {
    final String first = new String("A");
    final String second = new String("A");
    final Equivalence<String> equivalence = MapBuilder.ReferenceStrength.WEAK.defaultEquivalence();
    assertThat(equivalence.test(first, second), is(false));
    assertThat(equivalence.test(first, first), is(true));
  }

  @Test
  public void testInitialCapacitySetting() {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.initialCapacity(32);
    assertThat(builder.getInitialCapacity(), is(32));
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(15));
    assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(0));
  }

  @ParameterizedTest
  @ValueSource(ints = {15, 0})
  public void testInvalidCapacitySetting(final int capacity) {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    final Exception thrown =
        assertThrows(IllegalArgumentException.class, () -> builder.initialCapacity(capacity));
    assertThat(thrown.getMessage(), is("The initial capacity must be a power of two"));
  }

  @Test
  public void testConcurrencyLevelSetting() {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.concurrencyLevel(4);
    assertThat(builder.getConcurrencyLevel(), is(4));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  public void testInvalidConcurrencyLevel(final int level) {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    final Exception thrown =
        assertThrows(IllegalArgumentException.class, () -> builder.concurrencyLevel(level));
    assertThat(thrown.getMessage(), is("The concurrency level must be greather than 0"));
  }

  @Test
  public void testReferenceStrengthSetting() {
    final MapBuilder<String, Integer> builder = MapBuilder.builder();
    builder.weakKeys();
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.WEAK));

    builder.strongKeys();
    assertThat(builder.getKeyReferenceStrength(), is(MapBuilder.ReferenceStrength.STRONG));
  }

  @Test
  public void testCustomHashFunction() {
    final HashFunction<String> customHash = key -> 42;
    final MapBuilder<String, Integer> builder =
        MapBuilder.<String, Integer>builder().hashFunction(customHash);

    assertThat(builder.getHashFunction().compute("anything"), is(42));
    assertThat(builder.getHashFunction().compute("something else"), is(42));
  }

  @Test
  public void testBasicPutAndGetOrPutExisting() {
    final Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();
    final AtomicInteger insertions = new AtomicInteger(0);
    String result =
        map.getOrPut(
            "key1",
            () -> {
              insertions.incrementAndGet();
              return "Value1";
            });

    assertThat(result, is("Value1"));
    assertThat(insertions.get(), is(1));
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
    assertThat(insertions.get(), is(1));
    assertThat(map.size(), is(1));
  }

  @Test
  public void testGetNotFound() {
    final Map<String, Integer> map = MapBuilder.<String, Integer>builder().build();

    assertThat(map.get("something"), nullValue());
  }

  @Test
  public void testTooSmallInitialCapacity() {
    final Map<String, Integer> map =
        MapBuilder.<String, Integer>builder().concurrencyLevel(4).initialCapacity(2).build();

    assertThat(map.capacity(), is(4));
  }

  @Test
  public void testMultiplePutsAndSize() {
    final Map<Integer, String> map =
        MapBuilder.<Integer, String>builder().initialCapacity(8).build();

    for (int i = 0; i < 3; i++) {
      final int key = i + 1;
      map.getOrPut(key, () -> "Val" + key);
    }

    assertThat(map.size(), is(3));
    assertThat(map.capacity(), is(8));
    assertThat(map.getOrPut(2, () -> "ShouldNotBeCalled"), is("Val2"));
  }

  @Test
  public void testRehashing() {
    final Map<Integer, Integer> map =
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
  public void testMultipleRehashes() {
    final Map<String, String> map = MapBuilder.<String, String>builder().build();
    final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    final int size = 400;

    final String[] keys = new String[size];
    final String[] values = new String[size];

    for (int i = 0; i < size; ++i) {
      final StringBuilder builder = new StringBuilder(i + 1);
      for (int j = 0; j < i + 1; ++j) builder.append(alphabet.charAt(j % alphabet.length()));
      keys[i] = builder.toString();
      builder.setLength(0);

      for (int j = 0; j < i + 1; ++j) builder.append(alphabet.charAt((j + 4) % alphabet.length()));
      values[i] = builder.toString();
    }

    for (int i = 0; i < size; ++i) {
      final String value = values[i];
      assertThat(map.getOrPut(keys[i], () -> value), is(value));
    }

    assertThat(map.size(), is(size));
    assertThat(map.capacity(), is(1024));
    for (int i = 0; i < size; ++i) {
      assertThat(map.contains(keys[i]), is(true));
      assertThat(map.get(keys[i]), is(sameInstance(values[i])));
    }
  }

  @Test
  public void testCollisionHandling() {
    final Map<String, String> map =
        MapBuilder.<String, String>builder()
            .initialCapacity(4)
            .hashFunction((String key) -> 1)
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
    final Map<Object, String> map =
        MapBuilder.<Object, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(4)
            .build();

    Object key = new Object();
    final WeakReference<Object> weakKey = new WeakReference<>(key);
    String result = map.getOrPut(key, () -> "first-value");
    assertThat(result, is("first-value"));
    assertThat(map.size(), is(1));
    key = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    final Object newKey = new Object();
    result = map.getOrPut(newKey, () -> "second-value");

    assertThat(result, is("second-value"));
  }

  @Test
  public void testWeakKeysMultipleCollisions() {
    final Map<String, String> map =
        MapBuilder.<String, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(8)
            .build();

    final String key1 = new String("first-key");
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
    final Map<String, String> map =
        MapBuilder.<String, String>builder()
            .hashFunction(value -> 1)
            .weakKeys()
            .initialCapacity(8)
            .loadFactor(0.5f)
            .build();

    final String key1 = new String("first-key");
    String result = map.getOrPut(key1, () -> "first-value");
    assertThat(result, is("first-value"));
    assertThat(map.size(), is(1));

    String key2 = new String("second-key");
    result = map.getOrPut(key2, () -> "second-value");
    assertThat(result, is("second-value"));
    assertThat(map.size(), is(2));
    final WeakReference<String> reference = new WeakReference<>(key2);
    key2 = null;
    while (reference.get() != null) {
      System.gc();
    }

    final String key3 = new String("third-key");
    result = map.getOrPut(key3, () -> "third-value");
    assertThat(result, is("third-value"));
    assertThat(map.size(), greaterThanOrEqualTo(2));
    assertThat(map.size(), lessThanOrEqualTo(3));
  }

  @Test
  public void testEmptyMapHasNoNext() {
    final Map<Integer, String> map = MapBuilder.<Integer, String>builder().build();
    final Map.Iterator<Integer, String> it = map.iterator();
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testSingleEntryIteration() {
    final Map<Integer, String> map = MapBuilder.<Integer, String>builder().build();
    map.getOrPut(10, () -> "Ten");

    final Map.Iterator<Integer, String> it = map.iterator();
    assertThat(it.hasNext(), is(true));
    assertThat(it.key(), is(10));
    assertThat(it.value(), is("Ten"));

    it.next();
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testMultipleEntryIteration() {
    final Map<Integer, String> map =
        MapBuilder.<Integer, String>builder().initialCapacity(4).build();
    map.getOrPut(1, () -> "A");
    map.getOrPut(2, () -> "B");
    map.getOrPut(3, () -> "C");
    map.getOrPut(4, () -> "D");

    final Map.Iterator<Integer, String> it = map.iterator();
    final Set<Integer> keysFound = new HashSet<>();

    while (it.hasNext()) {
      keysFound.add(it.key());
      it.next();
    }

    assertThat(keysFound.size(), is(4));
    assertThat(keysFound.contains(1), is(true));
    assertThat(keysFound.contains(2), is(true));
    assertThat(keysFound.contains(3), is(true));
    assertThat(keysFound.contains(4), is(true));
  }

  @Test
  public void testWeakKeyEvictionDuringIteration() {
    final String key1 = "Keep me Strong";
    String key2 = new String("I will be collected");
    final String key3 = "Keep me also Strong";
    final Map<String, String> map =
        MapBuilder.<String, String>builder().initialCapacity(4).weakKeys().build();

    map.getOrPut(key1, () -> "Value1");
    map.getOrPut(key2, () -> "Value2");
    map.getOrPut(key3, () -> "Value3");

    final WeakReference<String> weakKey = new WeakReference<>(key2);
    key2 = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    final Map.Iterator<String, String> it = map.iterator();
    final Set<String> valuesFound = new HashSet<>();
    final Set<String> keysFound = new HashSet<>();

    while (it.hasNext()) {
      final String key = it.key();
      if (key != null) keysFound.add(key);
      valuesFound.add(it.value());
      it.next();
    }

    assertThat(keysFound.size(), is(2));
    assertThat(keysFound.contains(key1), is(true));
    assertThat(keysFound.contains(key3), is(true));

    assertThat(valuesFound.size(), is(3));
    assertThat(valuesFound.contains("Value1"), is(true));
    assertThat(valuesFound.contains("Value2"), is(true));
    assertThat(valuesFound.contains("Value3"), is(true));
  }

  @Test
  public void testContainsFound() {
    final String key = "somekey";
    final Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();

    map.getOrPut(key, () -> "Value1");
    assertThat(map.contains(key), is(true));
  }

  @Test
  public void testContainsNotFound() {
    final Map<String, String> map = MapBuilder.<String, String>builder().initialCapacity(4).build();

    assertThat(map.contains("somekey"), is(false));
  }

  @Test
  public void testContainsCollectedKey() {
    String key = new String("I will be collected");
    final Map<String, String> map =
        MapBuilder.<String, String>builder().initialCapacity(4).weakKeys().build();

    map.getOrPut(key, () -> "Value1");
    final WeakReference<String> weakKey = new WeakReference<>(key);
    key = null;
    while (weakKey.get() != null) {
      System.gc();
    }

    for (int i = 0; i < 10; ++i)
      assertThat(map.contains(new String("I will be collected")), is(false));
  }

  @Test
  public void testCompaction() {
    Object[] objects = null;
    final AtomicInteger invocations = new AtomicInteger(0);
    final Map<Object, Integer> map =
        MapBuilder.<Object, Integer>builder()
            .initialCapacity(64)
            .weakKeys()
            .keyDeletionCallback(
                (value) -> {
                  invocations.incrementAndGet();
                })
            .build();

    for (int i = 0; i < 10; ++i) {
      objects = new Object[2048];
      for (int j = 0; j < objects.length; ++j) {
        objects[j] = new Object();
        map.getOrPut(objects[j], () -> 2);
      }

      final WeakReference<Object> reference = new WeakReference<>(objects);
      objects = null;
      while (reference.get() != null) {
        System.gc();
      }
    }

    assertThat(invocations.get(), greaterThan(0));
  }
}
