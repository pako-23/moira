package moira.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SuspendManglerTest {
  @Mock private MethodVisitor methodVisitorMock;
  private SuspendMangler mangler;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    mangler = new SuspendMangler(methodVisitorMock, Opcodes.ACC_PUBLIC, "method", "()V");
  }

  @Test
  public void testOnMethodEnter() {
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      mangler.onMethodEnter();
      final List<Label> labels = mocked.constructed();
      assertEquals(1, labels.size());
      final InOrder order = inOrder(methodVisitorMock);
      order.verify(methodVisitorMock).visitLabel(labels.get(0));
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "suspend", "()V", false);
      order.verifyNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {Opcodes.RETURN, Opcodes.ARETURN, Opcodes.IRETURN, Opcodes.FRETURN})
  public void testOnMethodExitReturn(final int opcode) {
    mangler.onMethodExit(opcode);
    verify(methodVisitorMock)
        .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "resume", "()V", false);
    verifyNoMoreInteractions(methodVisitorMock);
  }

  @Test
  public void testOnMethodExitThrow() {
    mangler.onMethodExit(Opcodes.ATHROW);
    verifyNoInteractions(methodVisitorMock);
  }

  @Test
  public void testVisitMaxs() {
    try (MockedConstruction<Label> mocked = mockConstruction(Label.class)) {
      mangler.onMethodEnter();

      assertEquals(1, mocked.constructed().size());
      final InOrder order = inOrder(methodVisitorMock);
      order.verify(methodVisitorMock).visitLabel(mocked.constructed().get(0));
      order
          .verify(methodVisitorMock)
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "suspend", "()V", false);
      order.verifyNoMoreInteractions();

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
          .visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, "resume", "()V", false);
      order.verify(methodVisitorMock).visitInsn(Opcodes.ATHROW);
      order.verify(methodVisitorMock).visitMaxs(10, 12);
      order.verifyNoMoreInteractions();
    }
  }
}
