package moira.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

class Transformer implements ClassFileTransformer {
  private static final String[] FILTER_PREFIXES = {
    "java/io/",
    "java/lang/",
    "java/net/",
    "java/nio/",
    "java/text/",
    "java/util/concurrent/locks/",
    "jdk/",
    "junit/",
    "moira/",
    "org/junit/",
    "org/objectweb/asm/",
    "sun/",
  };
  private static String[] SUSPEND_PREFIXES = {
    "java/lang/ClassLoader",
    "java/lang/invoke/MethodHandleNatives",
    "java/net/URLClassLoader",
    "java/security/SecureClassLoader",
  };

  private final String profiler;

  public Transformer(final String profiler) {
    this.profiler = profiler;
  }

  private boolean prefixMatch(final String item, final String[] prefixes) {
    for (final String prefix : prefixes) {
      if (item.startsWith(prefix)) return true;
    }

    return false;
  }

  @Override
  public byte[] transform(
      final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classFileBuffer)
      throws IllegalClassFormatException {
    boolean suspend = false;
    if (className != null) {
      suspend = prefixMatch(className, SUSPEND_PREFIXES);
      if (!suspend && prefixMatch(className, FILTER_PREFIXES)) return null;
    }

    try {
      final ClassReader reader = new ClassReader(classFileBuffer);
      final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
      reader.accept(new ClassMangler(writer, suspend, profiler), ClassReader.EXPAND_FRAMES);
      return writer.toByteArray();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }
}
