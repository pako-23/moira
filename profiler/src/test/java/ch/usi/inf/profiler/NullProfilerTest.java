package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

public class NullProfilerTest {
  private static final String DUMMY_FIELD = "testField";
  private static final Object DUMMY_OBJECT = new Object();
  private static final Object[] DUMMY_ARRAY = new Object[1];
  private static final int DUMMY_INDEX = 0;
  private static final String DUMMY_TEST_NAME = "MyTest";

  @Test
  public void testWriteStaticField() {
    assertDoesNotThrow(() -> NullProfiler.writeStaticField(DUMMY_FIELD));
  }

  @Test
  public void testWriteArrayElement() {
    assertDoesNotThrow(() -> NullProfiler.writeArrayElement(DUMMY_ARRAY, DUMMY_INDEX));
  }

  @Test
  public void testWriteObjectField() {
    assertDoesNotThrow(() -> NullProfiler.writeObjectField(DUMMY_OBJECT, DUMMY_FIELD));
  }

  @Test
  public void testReadStaticField() {
    assertDoesNotThrow(() -> NullProfiler.readStaticField(DUMMY_FIELD));
  }

  @Test
  public void testReadArrayElement() {
    assertDoesNotThrow(() -> NullProfiler.readArrayElement(DUMMY_ARRAY, DUMMY_INDEX));
  }

  @Test
  public void testReadObjectField() {
    assertDoesNotThrow(() -> NullProfiler.readObjectField(DUMMY_OBJECT, DUMMY_FIELD));
  }

  @Test
  public void testEnterTestMethod() {
    assertDoesNotThrow(() -> NullProfiler.enterTestMethod(DUMMY_TEST_NAME));
  }

  @Test
  public void testExitTestMethod() {
    assertDoesNotThrow(() -> NullProfiler.exitTestMethod());
  }

  @Test
  public void testDumpMethod() {
    assertDoesNotThrow(() -> NullProfiler.dump(null));
  }

  @Test
  public void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<NullProfiler> constructor = NullProfiler.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  @Test
  public void testFinalClass() {
    assertTrue(Modifier.isFinal(NullProfiler.class.getModifiers()));
  }
}
