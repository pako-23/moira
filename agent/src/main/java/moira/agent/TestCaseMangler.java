package moira.agent;

import org.objectweb.asm.AnnotationVisitor;
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

  private static final TestDetector detector = new TestDetector();

  private Label tryBegin;
  private boolean instrument;
  private final String className;
  private final String methodDescription;
  private final String methodName;
  private final String profiler;

  public TestCaseMangler(
      final MethodVisitor mv,
      final String profiler,
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    super(Opcodes.ASM9, mv, access, methodName, description);

    detector.registerClass(superName, className);
    instrument =
        detector.isJUnit3TestMethod(superName, access, className, methodName, description)
            || detector.isJUnit4TestMethod(className, methodName, description);
    this.className = className;
    this.methodDescription = description;
    this.methodName = methodName;
    this.profiler = profiler;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, boolean visible) {
    if (descriptor.equals("Lorg/junit/Test;")) {
      instrument = true;
      detector.registerJUint4TestMethod(this.className, this.methodName, this.methodDescription);
    }

    return mv.visitAnnotation(descriptor, visible);
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
          Opcodes.INVOKESTATIC, profiler, methodNames[0], methodDescriptions[0], false);
    }
  }

  @Override
  public void onMethodExit(final int opcode) {
    if (instrument && opcode != Opcodes.ATHROW) {
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, profiler, methodNames[1], methodDescriptions[1], false);
    }
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    if (instrument) {
      final Label tryEnd = new Label();
      mv.visitTryCatchBlock(tryBegin, tryEnd, tryEnd, null);
      mv.visitLabel(tryEnd);
      mv.visitFrame(F_NEW, 0, null, 1, new Object[] {"java/lang/Throwable"});
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC, profiler, methodNames[1], methodDescriptions[1], false);
      mv.visitInsn(Opcodes.ATHROW);
    }

    mv.visitMaxs(maxStack, maxLocals);
  }
}
