package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestCaseManglerTest {
  @Mock private MethodVisitor methodVisitorMock;
  @Mock private TestDetector detectorMock;
  private TestDetector detector;
  private Field detectorField;

  @BeforeEach
  public void setup() throws IllegalAccessException, NoSuchFieldException {
    MockitoAnnotations.openMocks(this);

    detectorField = TestCaseMangler.class.getDeclaredField("detector");
    detectorField.setAccessible(true);

    final Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(detectorField, detectorField.getModifiers() & ~Modifier.FINAL);
    detector = (TestDetector) detectorField.get(null);
    detectorField.set(null, detectorMock);
  }

  @AfterEach
  public void cleanup() throws IllegalAccessException, NoSuchFieldException {
    detectorField.set(null, detector);

    final Field modifiers = Field.class.getDeclaredField("modifiers");
    modifiers.setAccessible(true);
    modifiers.setInt(detectorField, detectorField.getModifiers() | Modifier.FINAL);

    detectorField.setAccessible(false);
    modifiers.setAccessible(false);
  }

  private void makeNonInstrumentedChecks(final TestCaseMangler mangler, final int opcode) {
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      mangler.visitCode();
      mangler.onMethodEnter();
      mangler.onMethodExit(opcode);
      mangler.visitMaxs(10, 12);
      final InOrder order = inOrder(methodVisitorMock);
      order.verify(methodVisitorMock).visitCode();
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
      assertThat(mocked.constructed().size(), is(0));
    }
  }

  private void makeInstrumentedChecks(final TestCaseMangler mangler, final int opcode) {
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      final InOrder order = inOrder(methodVisitorMock);

      mangler.visitCode();
      order.verify(methodVisitorMock).visitCode();
      assertThat(mocked.constructed().size(), is(1));
      order.verify(methodVisitorMock).visitLabel(mocked.constructed().get(0));

      mangler.onMethodEnter();
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "enable", "()V", false);

      mangler.onMethodExit(opcode);
      if (opcode != Opcodes.ATHROW)
        order
            .verify(methodVisitorMock)
            .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "disable", "()V", false);

      mangler.visitMaxs(10, 12);
      final List<Label> labels = mocked.constructed();
      assertThat(labels.size(), is(2));
      order
          .verify(methodVisitorMock)
          .visitTryCatchBlock(labels.get(0), labels.get(1), labels.get(1), null);
      order.verify(methodVisitorMock).visitLabel(labels.get(1));
      order
          .verify(methodVisitorMock)
          .visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "disable", "()V", false);
      order.verify(methodVisitorMock).visitInsn(Opcodes.ATHROW);
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitNonTest(final int opcode) {
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(false);
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(false);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "method",
            "()V");
    makeNonInstrumentedChecks(mangler, opcode);
  }

  private static Stream<Arguments> testVisitJUnit3TestParams() {
    return Stream.of(Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN)
        .flatMap(
            opcode ->
                Stream.of("testSomething", "runTest").map(name -> Arguments.of(opcode, name)));
  }

  @ParameterizedTest
  @MethodSource("testVisitJUnit3TestParams")
  public void testVisitJUnit3Test(final int opcode, final String methodName) {
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(true);
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(false);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "junit/framework/TestCase",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            methodName,
            "()V");
    makeInstrumentedChecks(mangler, opcode);
  }

  @Test
  public void testVisitJUnit3PrivateTest() {
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(false);
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(true);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PRIVATE,
            "com/example/Example",
            "testMethod",
            "()V");
    makeNonInstrumentedChecks(mangler, Opcodes.IRETURN);
  }

  @Test
  public void testVisitJUnit3NotTest() {
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(false);
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(true);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PRIVATE,
            "com/example/Example",
            "someMethod",
            "()V");
    makeNonInstrumentedChecks(mangler, Opcodes.IRETURN);
  }

  @Test
  public void testVisitJUnit3NonValidTestDescription() {
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(false);
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(true);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PRIVATE,
            "com/example/Example",
            "runTest",
            "(I)V");
    makeNonInstrumentedChecks(mangler, Opcodes.IRETURN);
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitJUnit4Test(final int opcode) {
    when(detectorMock.isJUnit3TestClass(anyString(), anyString())).thenReturn(false);
    when(detectorMock.isJUint4TestMethod(anyString(), anyString(), anyString())).thenReturn(true);

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "testSomething",
            "()V");
    makeInstrumentedChecks(mangler, opcode);
  }
}
