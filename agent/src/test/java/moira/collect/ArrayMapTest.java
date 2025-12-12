package moira.collect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
    assertThat(map.capacity(), is(TEST_CAPACITY));
    assertThat(map.size(), is(TEST_CAPACITY));
  }

  @Test
  public void testGetOrPutMisssing() {
    final String newValue = "TestValue";
    final String result = map.getOrPut(5, () -> newValue);

    assertThat(result, is(newValue));
  }

  @Test
  public void testGetOrPutExisting() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertThat(result, is(initialValue));
    result = map.getOrPut(key, () -> "SomeDifferentValue");
    assertThat(result, is(initialValue));
  }

  @Test
  public void testContainsFound() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertThat(result, is(initialValue));
    assertThat(map.contains(key), is(true));
  }

  @Test
  public void testContainsNotFound() {
    final int key = 3;
    final String initialValue = "InitialValue";
    String result = map.getOrPut(key, () -> initialValue);

    assertThat(result, is(initialValue));
    assertThat(map.contains(key + 1), is(false));
  }

  @Test
  public void testContainsEmpty() {
    final int key = 3;

    assertThat(map.contains(key), is(false));
  }

  @Test
  public void testGetInsertedValue() {
    final String initialValue = "value";

    map.getOrPut(3, () -> initialValue);
    assertThat(map.get(3), is(initialValue));
  }

  @Test
  public void testIteratorEmptyMap() {
    final Map.Iterator<Integer, String> it = map.iterator();

    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testIteratorSingleItem() {
    final String value = "Start";
    map.getOrPut(0, () -> value);

    final Map.Iterator<Integer, String> it = map.iterator();

    assertThat(it.hasNext(), is(true));
    assertThat(it.key(), is(0));
    assertThat(it.value(), is(value));
    it.next();
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testIteratorSkipInitialEmptyValues() {
    final String value = "MapValue";
    map.getOrPut(3, () -> value);

    final Map.Iterator<Integer, String> it = map.iterator();

    assertThat(it.hasNext(), is(true));
    assertThat(it.key(), is(3));
    assertThat(it.value(), is(value));
    it.next();
    assertThat(it.hasNext(), is(false));
  }

  @Test
  public void testIteratorMultipleValues() {
    map.getOrPut(0, () -> "V0");
    map.getOrPut(1, () -> "V1");
    map.getOrPut(5, () -> "V5");

    Map.Iterator<Integer, String> it = map.iterator();

    assertThat(it.key(), is(0));
    assertThat(it.value(), is("V0"));

    it.next();
    assertThat(it.key(), is(1));
    assertThat(it.value(), is("V1"));

    it.next();
    assertThat(it.key(), is(5));
    assertThat(it.value(), is("V5"));
    it.next();

    assertThat(it.hasNext(), is(false));
  }
}
