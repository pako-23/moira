package moira.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

class Transformer implements ClassFileTransformer {

  private final ManglerConfig config;

  public Transformer(final ManglerConfig config) {
    this.config = config;
  }

  @Override
  public byte[] transform(
      final ClassLoader loader,
      final String className,
      final Class<?> classBeingRedefined,
      final ProtectionDomain protectionDomain,
      final byte[] classFileBuffer)
      throws IllegalClassFormatException {
    if (className != null && !config.isSuspended(className) && !config.shouldMangle(className))
      return null;

    try {
      final ClassReader reader = new ClassReader(classFileBuffer);
      final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
      reader.accept(new ClassMangler(writer, config), ClassReader.EXPAND_FRAMES);
      return writer.toByteArray();
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }
}
