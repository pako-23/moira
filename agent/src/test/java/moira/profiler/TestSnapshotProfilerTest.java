package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestSnapshotProfilerTest {

  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME =
      new String[] {"TestSnapshotMyTest", "TestSnapshotMyTest2"};

  @BeforeEach
  public void setup() {
    TestSnapshotProfiler.setup();
    TestSnapshotProfiler.resume();
  }

  private List<String> makeDump(String fileName) {
    List<String> lines = null;
    fileName = "test-snapshot-prof-" + fileName;

    try {
      File file = new File(fileName);
      file.deleteOnExit();
      TestSnapshotProfiler.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }

  @Test
  public void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<TestSnapshotProfiler> constructor =
        TestSnapshotProfiler.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
  }

  @Test
  public void testFinalClass() {
    assertThat(Modifier.isFinal(TestSnapshotProfiler.class.getModifiers()), is(true));
  }

  @Test
  public void testEnterExitTestMethod() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.exitTestMethod();
  }

  @Test
  public void testInitialProfilerSetup() {
    TestSnapshotProfiler.readArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.writeArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.readStaticField(FIELD);
    TestSnapshotProfiler.writeStaticField(FIELD);
    TestSnapshotProfiler.readObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.writeObjectField(OBJECT, FIELD);
    assertThat(makeDump("initial-profiler-setup").size(), is(0));
  }

  @Test
  public void testSuspendedProfiler() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.suspend();
    TestSnapshotProfiler.readArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.writeArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.readStaticField(FIELD);
    TestSnapshotProfiler.writeStaticField(FIELD);
    TestSnapshotProfiler.readObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.writeObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();
    assertThat(makeDump("suspended").size(), is(0));
  }

  @Test
  public void testNullObjects() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readArrayElement(null, INDEX);
    TestSnapshotProfiler.writeArrayElement(null, INDEX);
    TestSnapshotProfiler.readObjectField(null, FIELD);
    TestSnapshotProfiler.writeObjectField(null, FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();
    assertThat(makeDump("null-objects").size(), is(0));
  }

  @Test
  public void testStaticDependency() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.writeStaticField(FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readStaticField(FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    List<String> lines = makeDump("static-field-dependency");
    List<String> expected =
        Stream.of(TEST_NAME[1] + " " + TEST_NAME[0]).sorted().collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testObjectDependency() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.writeObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    List<String> lines = makeDump("object-dependency");
    List<String> expected =
        Stream.of(TEST_NAME[1] + " " + TEST_NAME[0]).sorted().collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testArrayDependency() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.writeArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    List<String> lines = makeDump("array-dependency");
    List<String> expected =
        Stream.of(TEST_NAME[1] + " " + TEST_NAME[0]).sorted().collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testStaticDependencyDisabled() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.writeStaticField(FIELD);
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.readStaticField(FIELD);
    TestSnapshotProfiler.exitTestMethod();

    assertThat(makeDump("static-field-dependency-disabled").size(), is(0));
  }

  @Test
  public void testObjectDependencyDisabled() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.writeObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.readObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.exitTestMethod();

    assertThat(makeDump("object-dependency-disabled").size(), is(0));
  }

  @Test
  public void testArrayDependencyDisabled() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.writeArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.readArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.exitTestMethod();

    assertThat(makeDump("array-dependency-disabled").size(), is(0));
  }

  @Test
  public void testObjectDependencyOtherField() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.writeObjectField(OBJECT, FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readObjectField(OBJECT, FIELD + "o");
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    assertThat(makeDump("object-field-dependency").size(), is(0));
  }

  @Test
  public void testArrayDependencyOtherIndex() {
    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.writeArrayElement(ARRAY, INDEX);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    TestSnapshotProfiler.readArrayElement(ARRAY, INDEX + 1);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    assertThat(makeDump("array-field-dependency").size(), is(0));
  }

  @Test
  public void testManyObjects() {
    Object[] objects = new Object[1024];

    for (int i = 0; i < objects.length; ++i) objects[i] = new Object();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    for (int i = 0; i < objects.length; ++i)
      TestSnapshotProfiler.writeObjectField(objects[i], FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    for (int i = 0; i < objects.length; ++i)
      TestSnapshotProfiler.readObjectField(objects[i], FIELD);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    List<String> lines = makeDump("many-object-dependency");
    List<String> expected =
        Stream.of(TEST_NAME[1] + " " + TEST_NAME[0]).sorted().collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @Test
  public void testManyArrays() {
    Object[] arrays = new Object[1024];

    for (int i = 0; i < arrays.length; ++i) arrays[i] = new int[5];

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[0]);
    TestSnapshotProfiler.enable();
    for (int i = 0; i < arrays.length; ++i)
      TestSnapshotProfiler.writeArrayElement(arrays[i], INDEX);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    TestSnapshotProfiler.enterTestMethod(TEST_NAME[1]);
    TestSnapshotProfiler.enable();
    for (int i = 0; i < arrays.length; ++i) TestSnapshotProfiler.readArrayElement(arrays[i], INDEX);
    TestSnapshotProfiler.disable();
    TestSnapshotProfiler.exitTestMethod();

    List<String> lines = makeDump("many-array-dependency");
    List<String> expected =
        Stream.of(TEST_NAME[1] + " " + TEST_NAME[0]).collect(Collectors.toList());

    assertThat(lines, is(expected));
  }
}
