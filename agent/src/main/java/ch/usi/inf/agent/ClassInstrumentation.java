package ch.usi.inf.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ClassInstrumentation extends ClassVisitor {
  private static String[] SUSPENSION_LIST = {
    "java/lang/ClassLoader", "java/net/URLClassLoader", "java/security/SecureClassLoader"
  };

  private static int classFilter = Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM;
  private static int methodFilter =
      Opcodes.ACC_NATIVE | Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;
  private boolean instrument;
  private boolean isJunit3TestCase;
  private boolean suspend;
  private String className;

  public ClassInstrumentation(ClassVisitor cv) {
    super(Opcodes.ASM9, cv);
    isJunit3TestCase = false;
    instrument = true;
    suspend = false;
    className = null;
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    cv.visit(version, access, name, signature, superName, interfaces);
    instrument = (access & classFilter) == 0;
    className = name;
    isJunit3TestCase = superName.equals("junit/framework/TestCase");
    suspend = isSuspended();
  }

  private boolean isSuspended() {
    for (final String name : SUSPENSION_LIST) if (name.equals(this.className)) return true;

    return false;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

    if (mv == null || !instrument || name.equals("<clinit>") || (access & methodFilter) != 0)
      return mv;

    if (suspend) return new SuspensionVisitor(mv, access, name, desc);

    mv = new MethodInstrumentation(mv);
    return new TestCaseVisitor(mv, isJunit3TestCase, access, className, name, desc);
  }
}
