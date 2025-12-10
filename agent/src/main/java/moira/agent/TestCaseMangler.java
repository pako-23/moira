package moira.agent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class TestCaseMangler extends AdviceAdapter {
  private static String[] methodNames = {
    "enable", "disable",
  };

  private static String[] methodDescriptions = {
    "()V", "()V",
  };

  private static TestDetector detector = new TestDetector();

  private boolean instrument;
  private Label tryBegin;

  public TestCaseMangler(
      final MethodVisitor mv,
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);

    instrument =
        isJUint3TestMethod(superName, access, className, methodName, description)
            || detector.isJUint4TestMethod(className, methodName, description);
  }

  private boolean isJUint3TestMethod(
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    return detector.isJUnit3TestClass(superName, className)
        && ((methodName.startsWith("test") && (access & Opcodes.ACC_PUBLIC) != 0)
            || (methodName.equals("runTest") && description.equals("()V")));
  }

  @Override
  public void visitCode() {
    super.visitCode();
    if (instrument) {
      tryBegin = new Label();
      mv.visitLabel(tryBegin);
    }
  }

  @Override
  public void onMethodEnter() {
    if (instrument) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescriptions[0], false);
    }
  }

  @Override
  public void onMethodExit(int opcode) {
    if (instrument && opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
    }
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    if (instrument) {
      final Label tryEnd = new Label();
      mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
      mv.visitLabel(tryEnd);
      mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescriptions[1], false);
      mv.visitInsn(Opcodes.ATHROW);
    }

    mv.visitMaxs(maxStack, maxLocals);
  }
}
