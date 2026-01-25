package moira.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassMangler extends ClassVisitor {
  private static final int CLASS_FILTER =
      Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM | Opcodes.ACC_SYNTHETIC;
  private static final int METHOD_FILTER =
      Opcodes.ACC_NATIVE | Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;

  private boolean mangle;
  private boolean suspend;
  private String superName;
  private String className;
  private final String profiler;

  public ClassMangler(final ClassVisitor cv, final boolean suspend, final String profiler) {
    super(Opcodes.ASM9, cv);
    mangle = true;
    this.suspend = suspend;
    this.profiler = profiler;
  }

  @Override
  public void visit(
      final int version,
      final int access,
      final String name,
      final String signature,
      final String superName,
      final String[] interfaces) {
    cv.visit(version, access, name, signature, superName, interfaces);

    mangle = (access & CLASS_FILTER) == 0 && !superName.equals("java/lang/reflect/Proxy");
    className = name;
    this.superName = superName;
  }

  @Override
  public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String description,
      final String signature,
      final String[] exceptions) {
    final MethodVisitor visitor = cv.visitMethod(access, name, description, signature, exceptions);
    if (visitor == null || !mangle || (access & METHOD_FILTER) != 0) return visitor;

    if (suspend) return new SuspendMangler(visitor, profiler, access, name, description);

    return new TestCaseMangler(
        new FieldAccessMangler(visitor, profiler, superName, name),
        profiler,
        superName,
        access,
        className,
        name,
        description);
  }
}
