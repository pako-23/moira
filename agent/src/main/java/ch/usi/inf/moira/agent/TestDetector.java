package ch.usi.inf.moira.agent;

import ch.usi.inf.moira.collect.Map;
import ch.usi.inf.moira.collect.MapBuilder;
import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestDetector {
  private final Map<String, Boolean> junit3TestsCache;
  private final Map<String, Map<String, Void>> junit4TestsCache;

  public TestDetector() {
    junit3TestsCache = MapBuilder.<String, Boolean>builder().build();
    junit4TestsCache = MapBuilder.<String, Map<String, Void>>builder().build();
  }

  public boolean isJUnit3TestClass(final String superName, final String className) {
    final Boolean isTest = junit3TestsCache.get(className);
    if (isTest != null) return isTest;
    else if (superName == null || superName.equals("java/lang/Object"))
      return junit3TestsCache.getOrPut(className, () -> false);
    else if (superName.equals("junit/framework/TestCase"))
      return junit3TestsCache.getOrPut(className, () -> true);

    try (InputStream stream =
        ClassLoader.getSystemClassLoader().getResourceAsStream(superName + ".class")) {
      final ClassReader reader = new ClassReader(stream);

      final boolean isSuperTest = isJUnit3TestClass(reader.getSuperName(), reader.getClassName());
      junit3TestsCache.getOrPut(reader.getClassName(), () -> isSuperTest);
      return isSuperTest;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean isJUint4TestMethod(
      final String className, final String methodName, final String description) {
    if (className.equals("java/lang/Object")) return false;

    final Map<String, Void> methodCache = junit4TestsCache.get(className);
    if (methodCache != null) {
      return methodCache.contains(methodKey(methodName, description));
    }

    solveClassHierarcy(className);
    return junit4TestsCache.get(className).contains(methodKey(methodName, description));
  }

  private String methodKey(final String name, final String description) {
    return name + "#" + description;
  }

  private void solveClassHierarcy(final String className) {
    final Map<String, Void> inserted =
        junit4TestsCache.getOrPut(className, () -> MapBuilder.<String, Void>builder().build());

    try (InputStream stream =
        ClassLoader.getSystemClassLoader().getResourceAsStream(className + ".class")) {
      final ClassReader reader = new ClassReader(stream);

      if (!reader.getSuperName().equals("java/lang/Object")) {
        Map<String, Void> superTests = junit4TestsCache.get(reader.getSuperName());
        if (superTests == null) {
          solveClassHierarcy(reader.getSuperName());
          superTests = junit4TestsCache.get(reader.getSuperName());
        }

        Map.Iterator<String, Void> it = superTests.iterator();
        while (it.hasNext()) {
          inserted.getOrPut(it.key(), () -> null);
          it.next();
        }
      }

      reader.accept(
          new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String description,
                final String signature,
                final String[] exceptions) {
              return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(final String descriptor, boolean visible) {
                  if (descriptor.equals("Lorg/junit/Test;")) {
                    inserted.getOrPut(methodKey(name, description), () -> null);
                  }

                  return null;
                }
              };
            }
          },
          ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    } catch (IOException e) {
    }
  }
}
