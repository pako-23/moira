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

public class OnlineProfilerTest {

  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME = new String[] {"DOIMyTest", "DOIMyTest2"};

  @BeforeEach
  public void setup() {
    OnlineProfiler.setup();
  }

  private List<String> makeDump() {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();

    OnlineProfiler.dump(new PrintStream(output));

    return Stream.of(output.toString().split("\n"))
        .filter(line -> !line.isEmpty())
        .collect(Collectors.toList());
  }

  @Test
  public void testObjectDependencyOtherField() {
    OnlineProfiler.enterTestMethod(TEST_NAME[0]);
    OnlineProfiler.enable();
    OnlineProfiler.writeObjectField(OBJECT, FIELD);
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    OnlineProfiler.enterTestMethod(TEST_NAME[1]);
    OnlineProfiler.enable();
    OnlineProfiler.readObjectField(OBJECT, FIELD + "o");
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    OnlineProfiler.enterTestMethod(TEST_NAME[0]);
    OnlineProfiler.enable();
    OnlineProfiler.writeArrayElement(ARRAY, INDEX);
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    OnlineProfiler.enterTestMethod(TEST_NAME[1]);
    OnlineProfiler.enable();
    OnlineProfiler.readArrayElement(ARRAY, INDEX + 1);
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testGCObjectDependency() {
    Object[] objects = new Object[512];

    OnlineProfiler.enterTestMethod(TEST_NAME[0]);
    OnlineProfiler.enable();
    for (int i = 0; i < objects.length; ++i) {
      objects[i] = new Object();
      ObjectProfiler.writeObjectField(objects[i], FIELD);
    }
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(objects);
    objects = null;
    while (reference.get() != null) {
      System.gc();
    }

    OnlineProfiler.enterTestMethod(TEST_NAME[1]);
    OnlineProfiler.enable();
    OnlineProfiler.readObjectField(new Object(), FIELD);
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }

  @Test
  public void testGCArrayDependency() {
    int[][] items = new int[512][];

    OnlineProfiler.enterTestMethod(TEST_NAME[0]);
    OnlineProfiler.enable();
    for (int i = 0; i < 512; ++i) {
      items[i] = new int[10];
      OnlineProfiler.writeArrayElement(items, INDEX);
    }
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(items);
    items = null;
    while (reference.get() != null) {
      System.gc();
    }

    OnlineProfiler.enterTestMethod(TEST_NAME[1]);
    OnlineProfiler.enable();
    OnlineProfiler.readArrayElement(new int[10], INDEX);
    OnlineProfiler.disable();
    OnlineProfiler.exitTestMethod();

    assertThat(makeDump().size(), is(0));
  }
}
