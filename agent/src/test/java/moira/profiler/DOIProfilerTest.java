package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DOIProfilerTest {

  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME = new String[] {"DOIMyTest", "DOIMyTest2"};

  @BeforeEach
  public void setup() {
    DOIProfiler.setup();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "doi-prof-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      DOIProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  @Test
  public void testObjectDependencyOtherField() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.enable();
    DOIProfiler.writeObjectField(OBJECT, FIELD);
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.enable();
    DOIProfiler.readObjectField(OBJECT, FIELD + "o");
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    assertThat(makeDump("object-field-dependency").size(), is(0));
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.enable();
    DOIProfiler.writeArrayElement(ARRAY, INDEX);
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.enable();
    DOIProfiler.readArrayElement(ARRAY, INDEX + 1);
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    assertThat(makeDump("array-field-dependency").size(), is(0));
  }

  @Test
  public void testGCObjectDependency() {
    Object[] objects = new Object[512];

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.enable();
    for (int i = 0; i < objects.length; ++i) {
      objects[i] = new Object();
      ObjectProfiler.writeObjectField(objects[i], FIELD);
    }
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(objects);
    objects = null;
    while (reference.get() != null) {
      System.gc();
    }

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.enable();
    DOIProfiler.readObjectField(new Object(), FIELD);
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    assertThat(makeDump("object-gc-dependency").size(), is(0));
  }

  @Test
  public void testGCArrayDependency() {
    int[][] items = new int[512][];

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.enable();
    for (int i = 0; i < 512; ++i) {
      items[i] = new int[10];
      DOIProfiler.writeArrayElement(items, INDEX);
    }
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(items);
    items = null;
    while (reference.get() != null) {
      System.gc();
    }

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.enable();
    DOIProfiler.readArrayElement(new int[10], INDEX);
    DOIProfiler.disable();
    DOIProfiler.exitTestMethod();

    assertThat(makeDump("array-gc-dependency").size(), is(0));
  }
}
