package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ProfilerTest {
  private static final String FIELD = "testField";
  private static final Object OBJECT = new Object();
  private static final Object[] ARRAY = new Object[10];
  private static final int INDEX = 0;
  private static final String[] TEST_NAME = new String[] {"MyTest", "MyTest2"};

  private static void enable(final Class<?> clazz) {
    try {
      clazz.getMethod("enable").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void disable(final Class<?> clazz) {
    try {
      clazz.getMethod("disable").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void suspend(final Class<?> clazz) {
    try {
      clazz.getMethod("suspend").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void readArrayElement(final Class<?> clazz, final Object array, final int index) {
    try {
      clazz.getMethod("readArrayElement", Object.class, int.class).invoke(null, array, index);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void readObjectField(
      final Class<?> clazz, final Object object, final String field) {
    try {
      clazz.getMethod("readObjectField", Object.class, String.class).invoke(null, object, field);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void readStaticField(final Class<?> clazz, final String field) {
    try {
      clazz.getMethod("readStaticField", String.class).invoke(null, field);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void writeArrayElement(final Class<?> clazz, final Object array, final int index) {
    try {
      clazz.getMethod("writeArrayElement", Object.class, int.class).invoke(null, array, index);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void writeObjectField(
      final Class<?> clazz, final Object object, final String field) {
    try {
      clazz.getMethod("writeObjectField", Object.class, String.class).invoke(null, object, field);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void writeStaticField(final Class<?> clazz, final String field) {
    try {
      clazz.getMethod("writeStaticField", String.class).invoke(null, field);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void enterTestMethod(final Class<?> clazz, final String test) {
    try {
      clazz.getMethod("enterTestMethod", String.class).invoke(null, test);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static List<String> dump(final Class<?> clazz, final String prefix, final String suffix) {
    final String fileName = prefix + "-" + suffix;

    try {
      final File file = new File(fileName);
      file.deleteOnExit();
      clazz.getMethod("dump", String.class).invoke(null, fileName);

      return Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (final NoSuchMethodException
        | IllegalAccessException
        | InvocationTargetException
        | IOException e) {
      fail(e.getMessage());
      return null;
    }
  }

  private static void exitTestMethod(final Class<?> clazz) {
    try {
      clazz.getMethod("exitTestMethod").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void resume(final Class<?> clazz) {
    try {
      clazz.getMethod("resume").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  private static void setup(final Class<?> clazz) {
    try {
      clazz.getMethod("setup").invoke(null);
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      fail(e.getMessage());
    }
  }

  @BeforeEach
  public void setup() {
    profilers()
        .forEach(
            argument -> {
              final Class<?> profiler = (Class<?>) argument.get()[0];
              setup(profiler);
            });
  }

  private static Stream<Arguments> profilers() {
    return Stream.of(
        Arguments.of(ObjectProfiler.class, "obj-prof"),
        Arguments.of(DOIProfiler.class, "doi-prof"),
        Arguments.of(TestSnapshotProfiler.class, "test-snapshot-prof"));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testConstructorIsPrivate(final Class<?> profiler, final String prefix)
      throws NoSuchMethodException {
    final Constructor<?> constructor = profiler.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testFinalClass(final Class<?> profiler, final String prefix)
      throws NoSuchMethodException {
    assertThat(Modifier.isFinal(TestSnapshotProfiler.class.getModifiers()), is(true));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testEnterExitTestMethod(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    exitTestMethod(profiler);
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testInitialProfilerSetup(final Class<?> profiler, final String prefix) {
    readArrayElement(profiler, ARRAY, INDEX);
    writeArrayElement(profiler, ARRAY, INDEX);
    readStaticField(profiler, FIELD);
    writeStaticField(profiler, FIELD);
    readObjectField(profiler, OBJECT, FIELD);
    writeObjectField(profiler, OBJECT, FIELD);
    assertThat(dump(profiler, prefix, "initial-profiler-setup").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testSuspendedProfiler(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    suspend(profiler);
    readArrayElement(profiler, ARRAY, INDEX);
    writeArrayElement(profiler, ARRAY, INDEX);
    readStaticField(profiler, FIELD);
    writeStaticField(profiler, FIELD);
    readObjectField(profiler, OBJECT, FIELD);
    writeObjectField(profiler, OBJECT, FIELD);
    resume(profiler);
    disable(profiler);
    exitTestMethod(profiler);
    assertThat(dump(profiler, prefix, "suspended").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testNullObjects(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    readArrayElement(profiler, null, INDEX);
    writeArrayElement(profiler, null, INDEX);
    readObjectField(profiler, null, FIELD);
    writeObjectField(profiler, null, FIELD);
    disable(profiler);
    exitTestMethod(profiler);
    assertThat(dump(profiler, prefix, "null-objects").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testStaticDoubleWriteDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeStaticField(profiler, FIELD);
    writeStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "static-field-double-write-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testStaticWriteBeforeRead(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    writeStaticField(profiler, FIELD);
    readStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);
    assertThat(dump(profiler, prefix, "static-write-before-read-dependency").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testStaticDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "static-field-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testStaticDependencyInverted(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeStaticField(profiler, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "static-field-dependency-inverted");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "object-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectWriteBeforeRead(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    writeObjectField(profiler, OBJECT, FIELD);
    readObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "object-write-before-read").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectDoubleWriteDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeObjectField(profiler, OBJECT, FIELD);
    writeObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "object-double-write-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectDependencyInverted(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeObjectField(profiler, OBJECT, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "object-dependency-inverted");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectDifferent(final Class<?> profiler, final String prefix) {
    final String first = "first";
    final String second = "second";
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeObjectField(profiler, first, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readObjectField(profiler, second, FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "object-different").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayDoubleWriteDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeArrayElement(profiler, ARRAY, INDEX);
    writeArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "array-double-write-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayWriteBeforeRead(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    writeArrayElement(profiler, ARRAY, INDEX);
    readArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "array-write-before-read").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayDependency(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "array-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayDependencyInverted(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readArrayElement(profiler, ARRAY, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "array-dependency-inverted");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayDifferent(final Class<?> profiler, final String prefix) {
    final int[] first = new int[10];
    final int[] second = new int[10];
    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    writeArrayElement(profiler, first, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    readArrayElement(profiler, second, INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "array-different").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testStaticDependencyDisabled(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    writeStaticField(profiler, FIELD);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    readStaticField(profiler, FIELD);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "static-field-dependency-disabled").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testObjectDependencyDisabled(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    writeObjectField(profiler, OBJECT, FIELD);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    readObjectField(profiler, OBJECT, FIELD);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "object-dependency-disabled").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testArrayDependencyDisabled(final Class<?> profiler, final String prefix) {
    enterTestMethod(profiler, TEST_NAME[0]);
    writeArrayElement(profiler, ARRAY, INDEX);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    readArrayElement(profiler, ARRAY, INDEX);
    exitTestMethod(profiler);

    assertThat(dump(profiler, prefix, "array-dependency-disabled").size(), is(0));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testManyObjects(final Class<?> profiler, final String prefix) {
    final Object[] objects = new Object[1000];

    for (int i = 0; i < objects.length; ++i) objects[i] = new Object();

    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    for (int i = 0; i < objects.length; ++i) writeObjectField(profiler, objects[i], FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    for (int i = 0; i < objects.length; ++i) readObjectField(profiler, objects[i], FIELD);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "many-object-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }

  @ParameterizedTest
  @MethodSource("profilers")
  public void testManyArrays(final Class<?> profiler, final String prefix) {
    final Object[] arrays = new Object[1024];

    for (int i = 0; i < arrays.length; ++i) arrays[i] = new int[5];

    enterTestMethod(profiler, TEST_NAME[0]);
    enable(profiler);
    for (int i = 0; i < arrays.length; ++i) writeArrayElement(profiler, arrays[i], INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    enterTestMethod(profiler, TEST_NAME[1]);
    enable(profiler);
    for (int i = 0; i < arrays.length; ++i) readArrayElement(profiler, arrays[i], INDEX);
    disable(profiler);
    exitTestMethod(profiler);

    final List<String> lines = dump(profiler, prefix, "many-array-dependency");
    final List<String> expected =
        Stream.of("from: " + TEST_NAME[1] + ", to: " + TEST_NAME[0])
            .sorted()
            .collect(Collectors.toList());

    assertThat(lines, is(expected));
  }
}
