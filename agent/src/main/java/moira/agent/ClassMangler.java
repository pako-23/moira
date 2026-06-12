package moira.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassMangler extends ClassVisitor {
  private static final int CLASS_FILTER =
      Opcodes.ACC_INTERFACE | Opcodes.ACC_ENUM | Opcodes.ACC_SYNTHETIC;
  private static final int METHOD_FILTER =
      Opcodes.ACC_NATIVE | Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC;

  private boolean suspend;
  private boolean mangle;
  private String superName;
  private String className;
  private final ManglerConfig config;

  public ClassMangler(final ClassVisitor cv, final ManglerConfig config) {
    super(Opcodes.ASM9, cv);
    this.config = config;
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

    this.className = name;
    this.superName = superName;
    this.suspend = config.isSuspended(className);
    this.mangle =
        this.suspend
            || ((access & CLASS_FILTER) == 0
                && config.shouldMangle(className)
                && !superName.equals("java/lang/reflect/Proxy"));
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

    if (suspend)
      return new SuspendMangler(visitor, config.getProfiler(), access, name, description);

    return new TestCaseMangler(
        new FieldAccessMangler(visitor, config.getProfiler(), superName, name),
        config.getProfiler(),
        superName,
        access,
        className,
        name,
        description);
  }
}
