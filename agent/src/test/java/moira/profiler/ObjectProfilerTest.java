package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObjectProfilerTest {

  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME = new String[] {"MyTest", "MyTest2"};

  @BeforeEach
  public void setup() {
    ObjectProfiler.setup();
  }

  private List<String> makeDump() {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();

    ObjectProfiler.dump(new PrintStream(output));

    return Stream.of(output.toString().split("\n"))
        .filter(line -> !line.isEmpty())
        .collect(Collectors.toList());
  }

  @Test
  public void testObjectDependencyOtherField() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.writeObjectField(OBJECT, FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    ObjectProfiler.readObjectField(OBJECT, FIELD + "o");
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    final List<String> lines = makeDump();
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[0] + ", to: " + TEST_NAME[1])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.writeArrayElement(ARRAY, INDEX);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    ObjectProfiler.readArrayElement(ARRAY, INDEX + 1);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    final List<String> lines = makeDump();
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[0] + ", to: " + TEST_NAME[1])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testGCObjectDependency() {
    Object[] objects = new Object[1000];

    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    for (int i = 0; i < objects.length; ++i) {
      objects[i] = new Object();
      ObjectProfiler.writeObjectField(objects[i], FIELD);
    }
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(objects);
    objects = null;
    while (reference.get() != null) {
      System.gc();
    }

    objects = new Object[1000];
    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    for (int i = 0; i < objects.length; ++i) {
      objects[i] = new Object();
      ObjectProfiler.readObjectField(objects[i], FIELD);
    }
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testGCArrayDependency() {
    int[][] items = new int[1000][];

    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    for (int i = 0; i < items.length; ++i) {
      items[i] = new int[10];
      ObjectProfiler.writeArrayElement(items, INDEX);
    }
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(items);
    items = null;
    while (reference.get() != null) {
      System.gc();
    }

    items = new int[1000][];
    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    for (int i = 0; i < items.length; ++i) {
      items[i] = new int[10];
      ObjectProfiler.readArrayElement(items[i], INDEX);
    }
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }
}
