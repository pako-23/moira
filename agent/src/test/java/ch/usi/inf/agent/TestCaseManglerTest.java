package ch.usi.inf.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestCaseManglerTest {
  @Mock private MethodVisitor methodVisitorMock;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitNonTest(final int opcode) {
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "method",
            "()V");
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      mangler.visitCode();
      mangler.onMethodEnter();
      mangler.onMethodExit(opcode);
      mangler.visitMaxs(10, 12);
      final InOrder order = inOrder(methodVisitorMock);
      order.verify(methodVisitorMock).visitCode();
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
      assertEquals(0, mocked.constructed().size());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitJUnit3Test(final int opcode) {
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "junit/framework/TestCase",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "testMethod",
            "()V");
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      final InOrder order = inOrder(methodVisitorMock);

      mangler.visitCode();
      order.verify(methodVisitorMock).visitCode();
      assertEquals(1, mocked.constructed().size());
      order.verify(methodVisitorMock).visitLabel(mocked.constructed().get(0));

      mangler.onMethodEnter();
      order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#testMethod");
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(
              Opcodes.INVOKESTATIC,
              Agent.PROFILER,
              "enterTestMethod",
              "(Ljava/lang/String;)V",
              false);

      mangler.onMethodExit(opcode);
      if (opcode != Opcodes.ATHROW)
        order
            .verify(methodVisitorMock)
            .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "exitTestMethod", "()V", false);

      mangler.visitMaxs(10, 12);
      final List<Label> labels = mocked.constructed();
      assertEquals(2, labels.size());
      order
          .verify(methodVisitorMock)
          .visitTryCatchBlock(labels.get(0), labels.get(1), labels.get(1), null);
      order.verify(methodVisitorMock).visitLabel(labels.get(1));
      order
          .verify(methodVisitorMock)
          .visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "exitTestMethod", "()V", false);
      order.verify(methodVisitorMock).visitInsn(Opcodes.ATHROW);
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitJUnit4Test(final int opcode) {
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "testSomething",
            "()V");
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      final InOrder order = inOrder(methodVisitorMock);

      final AnnotationVisitor annotationVisitorMock = mock(AnnotationVisitor.class);
      when(methodVisitorMock.visitAnnotation("Lorg/junit/Test;", true))
          .thenReturn(annotationVisitorMock);

      final AnnotationVisitor annotationVisitor = mangler.visitAnnotation("Lorg/junit/Test;", true);
      order.verify(methodVisitorMock).visitAnnotation("Lorg/junit/Test;", true);
      assertSame(annotationVisitor, annotationVisitor);

      mangler.visitCode();
      order.verify(methodVisitorMock).visitCode();
      assertEquals(1, mocked.constructed().size());
      order.verify(methodVisitorMock).visitLabel(mocked.constructed().get(0));

      mangler.onMethodEnter();
      order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#testSomething");
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(
              Opcodes.INVOKESTATIC,
              Agent.PROFILER,
              "enterTestMethod",
              "(Ljava/lang/String;)V",
              false);

      mangler.onMethodExit(opcode);
      if (opcode != Opcodes.ATHROW)
        order
            .verify(methodVisitorMock)
            .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "exitTestMethod", "()V", false);

      mangler.visitMaxs(10, 12);
      final List<Label> labels = mocked.constructed();
      assertEquals(2, labels.size());
      order
          .verify(methodVisitorMock)
          .visitTryCatchBlock(labels.get(0), labels.get(1), labels.get(1), null);
      order.verify(methodVisitorMock).visitLabel(labels.get(1));
      order
          .verify(methodVisitorMock)
          .visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "exitTestMethod", "()V", false);
      order.verify(methodVisitorMock).visitInsn(Opcodes.ATHROW);
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.IRETURN, Opcodes.ATHROW, Opcodes.FRETURN})
  public void testVisitNonTestAnnotation(final int opcode) {
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "testSomething",
            "()V");
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      final InOrder order = inOrder(methodVisitorMock);

      final AnnotationVisitor annotationVisitorMock = mock(AnnotationVisitor.class);
      when(methodVisitorMock.visitAnnotation("Lcom/example/Annotation;", true))
          .thenReturn(annotationVisitorMock);

      final AnnotationVisitor annotationVisitor =
          mangler.visitAnnotation("Lcom/example/Annotation;", true);
      order.verify(methodVisitorMock).visitAnnotation("Lcom/example/Annotation;", true);
      assertSame(annotationVisitor, annotationVisitor);

      mangler.visitCode();
      order.verify(methodVisitorMock).visitCode();

      mangler.onMethodEnter();
      if (opcode != Opcodes.ATHROW) mangler.onMethodExit(opcode);

      mangler.visitMaxs(10, 12);
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
      assertEquals(0, mocked.constructed().size());
    }
  }

  @Test
  public void testRegisterTestsFilterFound() throws IOException {
    final File file = new File("register-tests-filter-found");

    file.deleteOnExit();
    Files.write(
        file.toPath(),
        Arrays.asList(
            "  com/example/Example#method  com/example/Example#method2",
            "   com/example/Example#method4"));

    TestCaseMangler.registerTestsFilter(file.getPath());
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "method4",
            "()V");
    mangler.visitAnnotation("Lorg/junit/Test;", true);
    mangler.onMethodEnter();
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitAnnotation("Lorg/junit/Test;", true);
    order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#method4");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "enterTestMethod",
            "(Ljava/lang/String;)V",
            false);
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testRegisterTestsFilterNotFound() throws IOException {
    final File file = new File("register-tests-filter-not-found");

    file.deleteOnExit();
    Files.write(
        file.toPath(),
        Arrays.asList(
            "  com/example/Example#method  com/example/Example#method2",
            "   com/example/Example#method4"));

    TestCaseMangler.registerTestsFilter(file.getPath());
    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "method3",
            "()V");
    mangler.visitAnnotation("Lorg/junit/Test;", true);
    mangler.onMethodEnter();
    verify(methodVisitorMock).visitAnnotation("Lorg/junit/Test;", true);
    verifyNoMoreInteractions(methodVisitorMock);
  }

  @Test
  public void testFailRegistration() {
    final PrintStream originalStderr = System.err;
    final ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
    String message = "";
    System.setErr(new PrintStream(errorBuffer));
    try {
      TestCaseMangler.registerTestsFilter("not-existing-path");
      message = errorBuffer.toString();
    } finally {
      System.setErr(originalStderr);
    }

    assertTrue(message.contains("Warning: failed to read tests filter:"));

    final TestCaseMangler mangler =
        new TestCaseMangler(
            methodVisitorMock,
            "java/lang/Object",
            Opcodes.ACC_PUBLIC,
            "com/example/Example",
            "method",
            "()V");
    mangler.visitAnnotation("Lorg/junit/Test;", true);
    mangler.onMethodEnter();
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitAnnotation("Lorg/junit/Test;", true);
    order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#method");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "enterTestMethod",
            "(Ljava/lang/String;)V",
            false);
    order.verifyNoMoreInteractions();
  }

  @AfterEach
  public void cleanup() {
    TestCaseMangler.clearTestsFilter();
  }
}
