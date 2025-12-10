package moira.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public final class SuspendMangler extends AdviceAdapter {
  private static String[] methodNames = {
    "suspend", "resume",
  };
  private static String[] methodDescriptions = {
    "()V", "()V",
  };

  private Label tryBegin;

  public SuspendMangler(
      final MethodVisitor mv, final int access, final String methodName, final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);
  }

  @Override
  public void onMethodEnter() {
    tryBegin = new Label();
    mv.visitLabel(tryBegin);
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescriptions[0], false);
  }

  @Override
  public void onMethodExit(final int opcode) {
    if (opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
    }
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    Label tryEnd = new Label();
    mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
    mv.visitLabel(tryEnd);
    mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
    mv.visitInsn(Opcodes.ATHROW);
    mv.visitMaxs(maxStack, maxLocals);
  }
}
