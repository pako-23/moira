package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
    ObjectProfiler.resume();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "obj-prof-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      ObjectProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  @Test
  public void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<ObjectProfiler> constructor = ObjectProfiler.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
  }

  @Test
  public void testFinalClass() {
    assertThat(Modifier.isFinal(ObjectProfiler.class.getModifiers()), is(true));
  }

  @Test
  public void testEnterExitTestMethod() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.exitTestMethod();
  }

  @Test
  public void testInitialProfilerSetup() {
    ObjectProfiler.readArrayElement(ARRAY, INDEX);
    ObjectProfiler.writeArrayElement(ARRAY, INDEX);
    ObjectProfiler.readStaticField(FIELD);
    ObjectProfiler.writeStaticField(FIELD);
    ObjectProfiler.readObjectField(OBJECT, FIELD);
    ObjectProfiler.writeObjectField(OBJECT, FIELD);
    assertThat(makeDump("initial-profiler-setup").size(), is(0));
  }

  @Test
  public void testSuspendedProfiler() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.suspend();
    ObjectProfiler.readArrayElement(ARRAY, INDEX);
    ObjectProfiler.writeArrayElement(ARRAY, INDEX);
    ObjectProfiler.readStaticField(FIELD);
    ObjectProfiler.writeStaticField(FIELD);
    ObjectProfiler.readObjectField(OBJECT, FIELD);
    ObjectProfiler.writeObjectField(OBJECT, FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();
    assertThat(makeDump("suspended").size(), is(0));
  }

  @Test
  public void testNullObjects() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.readArrayElement(null, INDEX);
    ObjectProfiler.writeArrayElement(null, INDEX);
    ObjectProfiler.readObjectField(null, FIELD);
    ObjectProfiler.writeObjectField(null, FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();
    assertThat(makeDump("null-objects").size(), is(0));
  }

  @Test
  public void testStaticDependency() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.writeStaticField(FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    ObjectProfiler.readStaticField(FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    List<String> lines = makeDump("static-field-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testObjectDependency() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.writeObjectField(OBJECT, FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    ObjectProfiler.readObjectField(OBJECT, FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    List<String> lines = makeDump("object-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testArrayDependency() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    ObjectProfiler.writeArrayElement(ARRAY, INDEX);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    ObjectProfiler.readArrayElement(ARRAY, INDEX);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    List<String> lines = makeDump("array-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testStaticDependencyDisabled() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.writeStaticField(FIELD);
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.readStaticField(FIELD);
    ObjectProfiler.exitTestMethod();

    assertThat(makeDump("static-field-dependency-disabled").size(), is(0));
  }

  @Test
  public void testObjectDependencyDisabled() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.writeObjectField(OBJECT, FIELD);
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.readObjectField(OBJECT, FIELD);
    ObjectProfiler.exitTestMethod();

    assertThat(makeDump("object-dependency-disabled").size(), is(0));
  }

  @Test
  public void testArrayDependencyDisabled() {
    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.writeArrayElement(ARRAY, INDEX);
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.readArrayElement(ARRAY, INDEX);
    ObjectProfiler.exitTestMethod();

    assertThat(makeDump("array-dependency-disabled").size(), is(0));
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

    List<String> lines = makeDump("object-field-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
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

    List<String> lines = makeDump("array-field-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
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

    assertThat(makeDump("object-gc-dependency").size(), is(0));
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

    assertThat(makeDump("array-gc-dependency").size(), is(0));
  }

  @Test
  public void testManyObjects() {
    Object[] objects = new Object[1000];

    for (int i = 0; i < objects.length; ++i) objects[i] = new Object();

    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    for (int i = 0; i < objects.length; ++i) ObjectProfiler.writeObjectField(objects[i], FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    for (int i = 0; i < objects.length; ++i) ObjectProfiler.readObjectField(objects[i], FIELD);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    List<String> lines = makeDump("many-object-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testManyArrays() {
    Object[] arrays = new Object[1024];

    for (int i = 0; i < arrays.length; ++i) arrays[i] = new int[5];

    ObjectProfiler.enterTestMethod(TEST_NAME[0]);
    ObjectProfiler.enable();
    for (int i = 0; i < arrays.length; ++i) ObjectProfiler.writeArrayElement(arrays[i], INDEX);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    ObjectProfiler.enterTestMethod(TEST_NAME[1]);
    ObjectProfiler.enable();
    for (int i = 0; i < arrays.length; ++i) ObjectProfiler.readArrayElement(arrays[i], INDEX);
    ObjectProfiler.disable();
    ObjectProfiler.exitTestMethod();

    List<String> lines = makeDump("many-array-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }
}
