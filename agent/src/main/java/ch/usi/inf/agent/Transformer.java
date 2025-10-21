package ch.usi.inf.agent;

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
    "ch/usi/inf/agent/",
    "ch/usi/inf/profiler/",
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
      return instrument(classFileBuffer);
    } catch (Throwable t) {
      t.printStackTrace();
      return classFileBuffer;
    }
  }

  private byte[] instrument(byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cr.accept(new ClassInstrumentation(cw), ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }
}
