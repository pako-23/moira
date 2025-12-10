package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestDetectorTest {
  private TestDetector detector;

  private static class JUnit3TestMiddle extends junit.framework.TestCase {}

  private static class JUnit3Test extends JUnit3TestMiddle {}

  private static class JUnit3TestBase extends JUnit3Test {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  private @interface DummyAnnotation {}

  private abstract static class JUnit4TestMiddle {
    @org.junit.Test
    @DummyAnnotation
    public abstract void testMethodInherited();
  }

  private static class JUnit4Test extends JUnit4TestMiddle {
    @Override
    @DummyAnnotation
    public void testMethodInherited() {}

    @org.junit.Test
    @DummyAnnotation
    public void testMethodSimple() {}
  }

  @BeforeEach
  public void setup() {
    detector = new TestDetector();
  }

  @Test
  public void testJUnit3TestClass() {
    assertThat(
        detector.isJUnit3TestClass("junit/framework/TestCase", "com/example/Example"), is(true));
  }

  @Test
  public void testJUnit3TestClassCached() {
    assertThat(
        detector.isJUnit3TestClass("junit/framework/TestCase", "com/example/Example"), is(true));
    assertThat(
        detector.isJUnit3TestClass("junit/framework/TestCase", "com/example/Example"), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = "java/lang/Object")
  @NullSource
  public void testNotJUnit3TestClass(final String superName) {
    assertThat(detector.isJUnit3TestClass(superName, "com/example/Example"), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = "java/lang/Object")
  @NullSource
  public void testNotJUnit3TestClassCached(final String superName) {
    assertThat(detector.isJUnit3TestClass(superName, "com/example/Example"), is(false));
    assertThat(detector.isJUnit3TestClass(superName, "com/example/Example"), is(false));
  }

  @Test
  public void testJUnit3TestHierarchy() {
    assertThat(
        detector.isJUnit3TestClass(
            JUnit3Test.class.getSuperclass().getName().replace(".", "/"),
            JUnit3Test.class.getName().replace(".", "/")),
        is(true));
  }

  @Test
  public void testJUnit3TestHierarchyOfThree() {
    assertThat(
        detector.isJUnit3TestClass(
            JUnit3TestBase.class.getSuperclass().getName().replace(".", "/"),
            JUnit3TestBase.class.getName().replace(".", "/")),
        is(true));
  }

  @Test
  public void testJUnit3TestHierarchyCaching() {
    assertThat(
        detector.isJUnit3TestClass(
            JUnit3Test.class.getSuperclass().getName().replace(".", "/"),
            JUnit3Test.class.getName().replace(".", "/")),
        is(true));
    assertThat(
        detector.isJUnit3TestClass(
            JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
            JUnit3TestMiddle.class.getName().replace(".", "/")),
        is(true));
  }

  @Test
  public void testJUnit3NonTestHierarchy() {
    assertThat(
        detector.isJUnit3TestClass(
            java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
            java.util.HashMap.class.getName().replace(".", "/")),
        is(false));
  }

  @Test
  public void testJUnit3NonTestHierarchyCaching() {
    assertThat(
        detector.isJUnit3TestClass(
            java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
            java.util.HashMap.class.getName().replace(".", "/")),
        is(false));
    assertThat(
        detector.isJUnit3TestClass(
            java.util.AbstractMap.class.getSuperclass().getName().replace(".", "/"),
            java.util.AbstractMap.class.getName().replace(".", "/")),
        is(false));
  }

  @Test
  public void testJUnit3FailRead() {
    assertThat(
        detector.isJUnit3TestClass("com/example/Example", "com/example/ExampleBase"), is(false));
  }

  @Test
  public void testJUnit4ObjectClass() {
    assertThat(detector.isJUint4TestMethod("java/lang/Object", "hashCode", "()I"), is(false));
  }

  @Test
  public void testJUnit4TestMethod() {
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodSimple", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4Hierarchy() {
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4HierarchySuperCaching() {
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4TestMiddle.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4TestMethodCaching() {
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodSimple", "()V"),
        is(true));
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodSimple", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4HierarchyCaching() {
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
    assertThat(
        detector.isJUint4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4FailRead() {
    assertThat(detector.isJUint4TestMethod("com/example/Example", "someMethod", "()V"), is(false));
  }

  @Test
  public void testJUnit4NonTest() {
    assertThat(
        detector.isJUint4TestMethod(
            java.util.HashMap.class.getName().replace(".", "/"), "size", "()I"),
        is(false));
  }

  @Test
  public void testJUnit4NonTestCaching() {
    assertThat(
        detector.isJUint4TestMethod(
            java.util.HashMap.class.getName().replace(".", "/"), "size", "()I"),
        is(false));
  }
}
