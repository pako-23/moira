package ch.usi.inf.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

class SuspensionVisitor extends AdviceAdapter {
  private static String[] methodNames = {
    "suspend", "resume",
  };
  private static String[] methodDescs = {
    "()V", "()V",
  };

  private Label tryBegin;

  public SuspensionVisitor(
      MethodVisitor mv, int access, final String methodName, final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);
  }

  public void visitCode() {
    super.visitCode();
    tryBegin = new Label();
    mv.visitLabel(tryBegin);
  }

  protected void onMethodEnter() {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescs[0], false);
  }

  protected void onMethodExit(int opcode) {
    if (opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
    }
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    Label tryEnd = new Label();
    mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
    mv.visitLabel(tryEnd);
    mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
    mv.visitInsn(Opcodes.ATHROW);
    mv.visitMaxs(maxStack, maxLocals);
  }
}
