package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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
  public void setUp() {
    DOIProfiler.setup();
    DOIProfiler.resume();
  }

  private List<String> makeDump(final String fileName) {
    List<String> lines = null;

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
  public void testEnterTestMethod() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
  }

  @Test
  public void testExitTestMethod() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.exitTestMethod();
  }

  @Test
  public void testInitialProfilerSetup() {
    DOIProfiler.readArrayElement(ARRAY, INDEX);
    DOIProfiler.writeArrayElement(ARRAY, INDEX);
    DOIProfiler.readStaticField(FIELD);
    DOIProfiler.writeStaticField(FIELD);
    DOIProfiler.readObjectField(OBJECT, FIELD);
    DOIProfiler.writeObjectField(OBJECT, FIELD);
    assertEquals(0, makeDump("initial-profiler-setup").size());
  }

  @Test
  public void testSuspendedProfiler() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.suspend();
    DOIProfiler.readArrayElement(ARRAY, INDEX);
    DOIProfiler.writeArrayElement(ARRAY, INDEX);
    DOIProfiler.readStaticField(FIELD);
    DOIProfiler.writeStaticField(FIELD);
    DOIProfiler.readObjectField(OBJECT, FIELD);
    DOIProfiler.writeObjectField(OBJECT, FIELD);
    assertEquals(0, makeDump("suspended").size());
  }

  @Test
  public void testNullObjects() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.readArrayElement(null, INDEX);
    DOIProfiler.writeArrayElement(null, INDEX);
    DOIProfiler.readObjectField(null, FIELD);
    DOIProfiler.writeObjectField(null, FIELD);
    assertEquals(0, makeDump("null-objects").size());
  }

  @Test
  public void testStaticDependency() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.writeStaticField(FIELD);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readStaticField(FIELD);
    DOIProfiler.exitTestMethod();

    List<String> lines = makeDump("static-field-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertEquals(expected, lines);
  }

  @Test
  public void testObjectDependency() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.writeObjectField(OBJECT, FIELD);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readObjectField(OBJECT, FIELD);
    DOIProfiler.exitTestMethod();

    List<String> lines = makeDump("object-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertEquals(expected, lines);
  }

  @Test
  public void testArrayDependency() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.writeArrayElement(ARRAY, INDEX);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readArrayElement(ARRAY, INDEX);
    DOIProfiler.exitTestMethod();

    List<String> lines = makeDump("array-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertEquals(expected, lines);
  }

  @Test
  public void testObjectDependencyOtherField() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.writeObjectField(OBJECT, FIELD);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readObjectField(OBJECT, FIELD + "o");
    DOIProfiler.exitTestMethod();

    assertEquals(0, makeDump("object-field-dependency").size());
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    DOIProfiler.writeArrayElement(ARRAY, INDEX);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readArrayElement(ARRAY, INDEX + 1);
    DOIProfiler.exitTestMethod();

    assertEquals(0, makeDump("array-field-dependency").size());
  }

  @Test
  public void testGCObjectDependency() {
    Object[] objects = new Object[512];

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    for (int i = 0; i < objects.length; ++i) {
      objects[i] = new Object();
      ObjectProfiler.writeObjectField(objects[i], FIELD);
    }
    DOIProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(objects);
    objects = null;
    while (reference.get() != null) {
      System.gc();
    }

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readObjectField(new Object(), FIELD);
    DOIProfiler.exitTestMethod();

    assertEquals(0, makeDump("object-gc-dependency").size());
  }

  @Test
  public void testGCArrayDependency() {
    int[][] items = new int[512][];

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    for (int i = 0; i < 512; ++i) {
      items[i] = new int[10];
      DOIProfiler.writeArrayElement(items, INDEX);
    }
    DOIProfiler.exitTestMethod();

    WeakReference<Object> reference = new WeakReference<>(items);
    items = null;
    while (reference.get() != null) {
      System.gc();
    }

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    DOIProfiler.readArrayElement(new int[10], INDEX);
    DOIProfiler.exitTestMethod();

    assertEquals(0, makeDump("array-gc-dependency").size());
  }

  @Test
  public void testManyObjects() {
    Object[] objects = new Object[1024];

    for (int i = 0; i < objects.length; ++i) objects[i] = new Object();

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    for (int i = 0; i < objects.length; ++i) DOIProfiler.writeObjectField(objects[i], FIELD);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    for (int i = 0; i < objects.length; ++i) DOIProfiler.readObjectField(objects[i], FIELD);
    DOIProfiler.exitTestMethod();

    List<String> lines = makeDump("many-object-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertEquals(expected, lines);
  }

  @Test
  public void testManyArrays() {
    Object[] arrays = new Object[1024];

    for (int i = 0; i < arrays.length; ++i) arrays[i] = new int[5];

    DOIProfiler.enterTestMethod(TEST_NAME[0]);
    for (int i = 0; i < arrays.length; ++i) DOIProfiler.writeArrayElement(arrays[i], INDEX);
    DOIProfiler.exitTestMethod();

    DOIProfiler.enterTestMethod(TEST_NAME[1]);
    for (int i = 0; i < arrays.length; ++i) DOIProfiler.readArrayElement(arrays[i], INDEX);
    DOIProfiler.exitTestMethod();

    List<String> lines = makeDump("many-array-dependency");
    List<String> expected =
        Arrays.asList(TEST_NAME[1] + " " + TEST_NAME[0]).stream()
            .sorted()
            .collect(Collectors.toList());

    assertEquals(expected, lines);
  }
}
