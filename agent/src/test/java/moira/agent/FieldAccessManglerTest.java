package moira.agent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FieldAccessManglerTest {
  @Mock private MethodVisitor methodVisitorMock;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  private static <T> Stream<Arguments> buildSuperClassStream(final T[] items) {
    final List<Arguments> list = new ArrayList<>();

    for (final T item : items) {
      list.add(Arguments.of(null, item));
      list.add(Arguments.of("java/lang/Object", item));
      list.add(Arguments.of("com/example/Super", item));
    }

    return list.stream();
  }

  private static Stream<Arguments> testVisitPutfieldWordMangleParam() {
    return buildSuperClassStream(new String[] {"D", "J"});
  }

  private static Stream<Arguments> testVisitPutfieldMangleParam() {
    return buildSuperClassStream(new String[] {"I", "Ljava/lang/String;", "F", "Z"});
  }

  private static Stream<Arguments> testVisitInsnStoreWordMangledParams() {
    return buildSuperClassStream(new Integer[] {Opcodes.DASTORE, Opcodes.LASTORE});
  }

  private static Stream<Arguments> testVisitStaticFieldsParams() {
    return buildSuperClassStream(new String[] {"methodName", "<init>"});
  }

  private static Stream<Arguments> testObjectFieldParams() {
    return buildSuperClassStream(new Integer[] {Opcodes.GETFIELD, Opcodes.PUTFIELD});
  }

  private static Stream<Arguments> testVisitInsnStoreMangledParams() {
    return buildSuperClassStream(
        new Integer[] {
          Opcodes.IASTORE,
          Opcodes.FASTORE,
          Opcodes.BASTORE,
          Opcodes.CASTORE,
          Opcodes.AASTORE,
          Opcodes.SASTORE
        });
  }

  private static Stream<Arguments> testVisitInsnLoadMangledParams() {
    return buildSuperClassStream(
        new Integer[] {
          Opcodes.DALOAD,
          Opcodes.LALOAD,
          Opcodes.IALOAD,
          Opcodes.FALOAD,
          Opcodes.BALOAD,
          Opcodes.CALOAD,
          Opcodes.AALOAD,
          Opcodes.SALOAD,
        });
  }

  private static Stream<Arguments> testVisitInsnNoMangleParams() {
    return buildSuperClassStream(
        new Integer[] {
          Opcodes.DUP, Opcodes.DUP2_X1, Opcodes.DUP2,
        });
  }

  @ParameterizedTest
  @MethodSource("testVisitInsnStoreWordMangledParams")
  public void testVisitInsnStoreWordMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP2_X2);
    order.verify(methodVisitorMock).visitInsn(Opcodes.POP2);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP2_X2);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeArrayElement",
            "(Ljava/lang/Object;I)V",
            false);
    order.verify(methodVisitorMock).visitInsn(opcode);
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitInsnStoreMangledParams")
  public void testVisitInsnStoreMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP_X2);
    order.verify(methodVisitorMock).visitInsn(Opcodes.POP);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP2_X1);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeArrayElement",
            "(Ljava/lang/Object;I)V",
            false);
    order.verify(methodVisitorMock).visitInsn(opcode);
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitInsnLoadMangledParams")
  public void testVisitInsnLoadMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    final InOrder order = inOrder(methodVisitorMock);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "readArrayElement",
            "(Ljava/lang/Object;I)V",
            false);
    order.verify(methodVisitorMock).visitInsn(opcode);
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitInsnNoMangleParams")
  public void testVisitInsnNoMangle(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    verify(methodVisitorMock).visitInsn(opcode);
    verifyNoMoreInteractions(methodVisitorMock);
  }

  @ParameterizedTest
  @MethodSource("testVisitPutfieldWordMangleParam")
  public void testVisitPutfieldWordMangle(final String superName, final String description) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");

    mangler.visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", description);
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP2_X1);
    order.verify(methodVisitorMock).visitInsn(Opcodes.POP2);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP_X2);
    order.verify(methodVisitorMock).visitLdcInsn("field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeObjectField",
            "(Ljava/lang/Object;Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", description);
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitPutfieldMangleParam")
  public void testVisitPutfieldMangle(final String superName, final String description) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");

    mangler.visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", description);
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.SWAP);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP_X1);
    order.verify(methodVisitorMock).visitLdcInsn("field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeObjectField",
            "(Ljava/lang/Object;Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", description);
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @ValueSource(strings = {"java/lang/Object", "com/example/Super"})
  @NullSource
  public void testVisitGetfieldMangle(final String superName) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");

    mangler.visitFieldInsn(Opcodes.GETFIELD, "com/example/Example", "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP);
    order.verify(methodVisitorMock).visitLdcInsn("field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "readObjectField",
            "(Ljava/lang/Object;Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.GETFIELD, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitStaticFieldsParams")
  public void testVisitGetstaticMangle(final String superName, final String methodName) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, methodName);

    mangler.visitFieldInsn(Opcodes.GETSTATIC, "com/example/Example", "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "readStaticField",
            "(Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.GETSTATIC, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testVisitStaticFieldsParams")
  public void testVisitPutstaticMangle(final String superName, final String methodName) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, methodName);

    mangler.visitFieldInsn(Opcodes.PUTSTATIC, "com/example/Example", "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitLdcInsn("com/example/Example#field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeStaticField",
            "(Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.PUTSTATIC, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.GETFIELD, Opcodes.PUTFIELD})
  public void testNotInitializedObject(final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, "com/example/Example", "<init>");

    mangler.visitFieldInsn(opcode, "com/example/Example", "field", "");
    verify(methodVisitorMock).visitFieldInsn(opcode, "com/example/Example", "field", "");
    verifyNoMoreInteractions(methodVisitorMock);
  }

  @ParameterizedTest
  @MethodSource("testObjectFieldParams")
  public void testInvokeInstruction(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "<init>");

    mangler.visitMethodInsn(Opcodes.INVOKESTATIC, "com/example/Example", "method", "()V", false);
    mangler.visitFieldInsn(opcode, "com/example/Example", "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(Opcodes.INVOKESTATIC, "com/example/Example", "method", "()V", false);
    order.verify(methodVisitorMock).visitFieldInsn(opcode, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @MethodSource("testObjectFieldParams")
  public void testInvokeSpecialNotInitialized(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "<init>");

    mangler.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/example/Example", "method", "()V", false);
    mangler.visitFieldInsn(opcode, "com/example/Example", "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(Opcodes.INVOKESPECIAL, "com/example/Example", "method", "()V", false);
    order.verify(methodVisitorMock).visitFieldInsn(opcode, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.GETFIELD, Opcodes.PUTFIELD})
  public void testInvokeSpecialSuperMethod(final int opcode) {
    final String superName = "com/example/Example";
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "<init>");

    mangler.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "method", "()V", false);
    mangler.visitFieldInsn(opcode, superName, "field", "");
    final InOrder order = inOrder(methodVisitorMock);
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "method", "()V", false);
    order.verify(methodVisitorMock).visitFieldInsn(opcode, superName, "field", "");
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testPutfieldObjectInitialization() {
    final String superName = "com/example/Super";
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "<init>");

    mangler.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
    mangler.visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", "I");

    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.SWAP);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP_X1);
    order.verify(methodVisitorMock).visitLdcInsn("field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "writeObjectField",
            "(Ljava/lang/Object;Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.PUTFIELD, "com/example/Example", "field", "I");
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testGetfieldObjectInitialization() {
    final String superName = "com/example/Super";
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "<init>");

    mangler.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
    mangler.visitFieldInsn(Opcodes.GETFIELD, "com/example/Example", "field", "");

    final InOrder order = inOrder(methodVisitorMock);
    order.verify(methodVisitorMock).visitInsn(Opcodes.DUP);
    order.verify(methodVisitorMock).visitLdcInsn("field");
    order
        .verify(methodVisitorMock)
        .visitMethodInsn(
            Opcodes.INVOKESTATIC,
            Agent.PROFILER,
            "readObjectField",
            "(Ljava/lang/Object;Ljava/lang/String;)V",
            false);
    order
        .verify(methodVisitorMock)
        .visitFieldInsn(Opcodes.GETFIELD, "com/example/Example", "field", "");
    order.verifyNoMoreInteractions();
  }
}
