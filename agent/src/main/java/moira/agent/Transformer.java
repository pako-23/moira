package moira.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

class Transformer implements ClassFileTransformer {
  private static final String[] filter = {
    "java/io/",
    "java/lang/",
    "java/nio/",
    "moira/",
    "jdk/",
    "junit/",
    "org/junit/",
    "org/objectweb/asm/",
    "sun/",
  };

  @Override
  public byte[] transform(
      final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classFileBuffer)
      throws IllegalClassFormatException {

    if (className != null) {
      for (int i = 0; i < filter.length; ++i) if (className.startsWith(filter[i])) return null;
    }

    try {
      return instrument(loader, classFileBuffer);
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

  private byte[] instrument(final ClassLoader loader, final byte[] bytes) {
    final ClassReader reader = new ClassReader(bytes);
    final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
    reader.accept(new ClassMangler(writer), ClassReader.EXPAND_FRAMES);
    return writer.toByteArray();
  }
}
