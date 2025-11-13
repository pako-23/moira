package ch.usi.inf.agent;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  private static Stream<Arguments> buildOpcodeStream(final int[] opcodes) {
    final List<Arguments> list = new ArrayList<>();

    for (final int opcode : opcodes) {
      list.add(Arguments.of(null, opcode));
      list.add(Arguments.of("java/lang/Object", opcode));
      list.add(Arguments.of("com/example/Super", opcode));
    }

    return list.stream();
  }

  private static Stream<Arguments> testVisitInstStoreWordMangledParams() {
    return buildOpcodeStream(new int[] {Opcodes.DASTORE, Opcodes.LASTORE});
  }

  private static Stream<Arguments> testVisitInstStoreMangledParams() {
    return buildOpcodeStream(
        new int[] {
          Opcodes.IASTORE,
          Opcodes.FASTORE,
          Opcodes.BASTORE,
          Opcodes.CASTORE,
          Opcodes.AASTORE,
          Opcodes.SASTORE
        });
  }

  private static Stream<Arguments> testVisitInstLoadMangledParams() {
    return buildOpcodeStream(
        new int[] {
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

  private static Stream<Arguments> testVisitInstNoMangleParams() {
    return buildOpcodeStream(
        new int[] {
          Opcodes.DUP, Opcodes.DUP2_X1, Opcodes.DUP2,
        });
  }

  @ParameterizedTest
  @MethodSource("testVisitInstStoreWordMangledParams")
  public void testVisitInstStoreWordMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    InOrder order = inOrder(methodVisitorMock);
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
  @MethodSource("testVisitInstStoreMangledParams")
  public void testVisitInstStoreMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    InOrder order = inOrder(methodVisitorMock);
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
  @MethodSource("testVisitInstLoadMangledParams")
  public void testVisitInstLoadMangled(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    InOrder order = inOrder(methodVisitorMock);
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
  @MethodSource("testVisitInstNoMangleParams")
  public void testVisitInstNoMangle(final String superName, final int opcode) {
    final FieldAccessMangler mangler =
        new FieldAccessMangler(methodVisitorMock, superName, "someMethod");
    mangler.visitInsn(opcode);
    verify(methodVisitorMock).visitInsn(opcode);
    verifyNoMoreInteractions(methodVisitorMock);
  }
}
