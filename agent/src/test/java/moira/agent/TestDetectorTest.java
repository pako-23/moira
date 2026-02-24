package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Opcodes;

public class TestDetectorTest {
  private TestDetector detector;

  private static class JUnit3TestMiddle extends junit.framework.TestCase {}

  private static class JUnit3Test extends JUnit3TestMiddle {}

  private static class JUnit3TestBase extends JUnit3Test {}

  private abstract static class JUnit4TestMiddle {
    @org.junit.Test
    public abstract void testMethodInherited();
  }

  private static class JUnit4Test extends JUnit4TestMiddle {
    @Override
    public void testMethodInherited() {}

    @org.junit.Test
    public void testMethodSimple() {}
  }

  @BeforeEach
  public void setup() {
    detector = new TestDetector();
  }

  @Test
  public void testJUnit3TestMethod() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(true));
  }

  @Test
  public void testJUnit3PrivateTestMethod() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PRIVATE, className, "testSomething", "()V"),
        is(false));
  }

  @Test
  public void testJUnit3RunTestOverrideMethod() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(superName, Opcodes.ACC_PUBLIC, className, "runTest", "()V"),
        is(true));
  }

  @Test
  public void testJUnit3RunTestInvalidOverrideMethod() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(superName, Opcodes.ACC_PUBLIC, className, "runTest", "(I)V"),
        is(false));
  }

  @Test
  public void testJUnit3TestClassNonTestMethod() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(superName, Opcodes.ACC_PRIVATE, className, "something", "()I"),
        is(false));
  }

  @Test
  public void testJUnit3TestClassCached() {
    final String superName = "junit/framework/TestCase";
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(true));
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = "java/lang/Object")
  @NullSource
  public void testNotJUnit3TestClass(final String superName) {
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = "java/lang/Object")
  @NullSource
  public void testNotJUnit3TestClassCached(final String superName) {
    final String className = "com/example/Example";

    detector.registerClass(superName, className);
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(false));
    assertThat(
        detector.isJUnit3TestMethod(
            superName, Opcodes.ACC_PUBLIC, className, "testSomething", "()V"),
        is(false));
  }

  @Test
  public void testJUnit3TestHierarchy() {
    detector.registerClass(
        JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
        JUnit3TestMiddle.class.getName().replace(".", "/"));

    assertThat(
        detector.isJUnit3TestMethod(
            JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            JUnit3TestMiddle.class.getName().replace(".", "/"),
            "testSomething",
            "()V"),
        is(true));
  }

  @Test
  public void testJUnit3TestHierarchyOfThree() {
    detector.registerClass(
        JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
        JUnit3TestMiddle.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit3Test.class.getSuperclass().getName().replace(".", "/"),
        JUnit3Test.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit3TestBase.class.getSuperclass().getName().replace(".", "/"),
        JUnit3TestBase.class.getName().replace(".", "/"));

    assertThat(
        detector.isJUnit3TestMethod(
            JUnit3TestBase.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            JUnit3TestBase.class.getName().replace(".", "/"),
            "testSomething",
            "()V"),
        is(true));
  }

  @Test
  public void testJUnit3TestHierarchyCaching() {
    detector.registerClass(
        JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
        JUnit3TestMiddle.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit3Test.class.getSuperclass().getName().replace(".", "/"),
        JUnit3Test.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit3TestBase.class.getSuperclass().getName().replace(".", "/"),
        JUnit3TestBase.class.getName().replace(".", "/"));

    assertThat(
        detector.isJUnit3TestMethod(
            JUnit3TestMiddle.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            JUnit3TestMiddle.class.getName().replace(".", "/"),
            "testSomething",
            "()V"),
        is(true));

    assertThat(
        detector.isJUnit3TestMethod(
            JUnit3TestBase.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            JUnit3TestBase.class.getName().replace(".", "/"),
            "testSomething",
            "()V"),
        is(true));
  }

  @Test
  public void testJUnit3NonTestHierarchy() {
    detector.registerClass(
        java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
        java.util.HashMap.class.getName().replace(".", "/"));

    assertThat(
        detector.isJUnit3TestMethod(
            java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            java.util.HashMap.class.getName().replace(".", "/"),
            "size",
            "()I"),
        is(false));
  }

  @Test
  public void testJUnit3NonTestHierarchyCaching() {
    detector.registerClass(
        java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
        java.util.HashMap.class.getName().replace(".", "/"));

    assertThat(
        detector.isJUnit3TestMethod(
            java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            java.util.HashMap.class.getName().replace(".", "/"),
            "size",
            "()I"),
        is(false));
    assertThat(
        detector.isJUnit3TestMethod(
            java.util.HashMap.class.getSuperclass().getName().replace(".", "/"),
            Opcodes.ACC_PUBLIC,
            java.util.HashMap.class.getName().replace(".", "/"),
            "size",
            "()I"),
        is(false));
  }

  @Test
  public void testJUnit4ObjectClass() {
    assertThat(detector.isJUnit4TestMethod("java/lang/Object", "hashCode", "()I"), is(false));
  }

  @Test
  public void testJUnit4TestMethod() {
    detector.registerClass(
        JUnit4Test.class.getSuperclass().getName().replace(".", "/"),
        JUnit4Test.class.getName().replace(".", "/"));
    detector.registerJUint4TestMethod(
        JUnit4Test.class.getName().replace(".", "/"), "testMethodSimple", "()V");

    assertThat(
        detector.isJUnit4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodSimple", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4Hierarchy() {
    detector.registerClass(
        JUnit4TestMiddle.class.getSuperclass().getName().replace(".", "/"),
        JUnit4TestMiddle.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit4Test.class.getSuperclass().getName().replace(".", "/"),
        JUnit4Test.class.getName().replace(".", "/"));
    detector.registerJUint4TestMethod(
        JUnit4TestMiddle.class.getName().replace(".", "/"), "testMethodInherited", "()V");

    assertThat(
        detector.isJUnit4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4HierarchySuperCaching() {
    detector.registerClass(
        JUnit4TestMiddle.class.getSuperclass().getName().replace(".", "/"),
        JUnit4TestMiddle.class.getName().replace(".", "/"));
    detector.registerClass(
        JUnit4Test.class.getSuperclass().getName().replace(".", "/"),
        JUnit4Test.class.getName().replace(".", "/"));
    detector.registerJUint4TestMethod(
        JUnit4TestMiddle.class.getName().replace(".", "/"), "testMethodInherited", "()V");

    assertThat(
        detector.isJUnit4TestMethod(
            JUnit4Test.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
    assertThat(
        detector.isJUnit4TestMethod(
            JUnit4TestMiddle.class.getName().replace(".", "/"), "testMethodInherited", "()V"),
        is(true));
  }

  @Test
  public void testJUnit4FailRead() {
    assertThat(detector.isJUnit4TestMethod("com/example/Example", "someMethod", "()V"), is(false));
  }

  @Test
  public void testJUnit4NonTest() {
    assertThat(
        detector.isJUnit4TestMethod(
            java.util.HashMap.class.getName().replace(".", "/"), "size", "()I"),
        is(false));
  }

  @Test
  public void testJUnit4NonTestCaching() {
    assertThat(
        detector.isJUnit4TestMethod(
            java.util.HashMap.class.getName().replace(".", "/"), "size", "()I"),
        is(false));
  }
}
