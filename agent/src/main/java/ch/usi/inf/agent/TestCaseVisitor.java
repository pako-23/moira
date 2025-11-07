package ch.usi.inf.agent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

class TestCaseVisitor extends AdviceAdapter {
  private static String[] methodNames = {
    "enterTestMethod", "exitTestMethod",
  };

  private static String[] methodDescs = {
    "(Ljava/lang/String;)V", "()V",
  };

  private boolean instrument;
  private Label tryBegin;
  private final String testCase;

  public TestCaseVisitor(
      MethodVisitor mv,
      boolean isJunit3TestCase,
      int access,
      final String className,
      final String methodName,
      final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);
    this.instrument = false;
    if (isJunit3TestCase) instrument = methodName.startsWith("test");
    this.testCase = className + "#" + methodName;
  }

  public void visitCode() {
    super.visitCode();
    if (instrument) {
      tryBegin = new Label();
      mv.visitLabel(tryBegin);
    }
  }

  protected void onMethodEnter() {
    if (instrument) {
      mv.visitLdcInsn(testCase);
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[0], methodDescs[0], false);
    }
  }

  protected void onMethodExit(int opcode) {
    if (instrument && opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
    }
  }

  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (descriptor.equals("Lorg/junit/Test;")) instrument = true;
    if (Agent.testsFilter != null && !Agent.testsFilter.contains(testCase)) instrument = false;

    return mv.visitAnnotation(descriptor, visible);
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    if (instrument) {
      Label tryEnd = new Label();
      mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
      mv.visitLabel(tryEnd);
      mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, Agent.PROFILER, methodNames[1], methodDescs[1], false);
      mv.visitInsn(Opcodes.ATHROW);
    }

    mv.visitMaxs(maxStack, maxLocals);
  }
}
