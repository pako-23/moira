package moira.agent;

import moira.collect.Map;
import moira.collect.MapBuilder;
import org.objectweb.asm.Opcodes;

public class TestDetector {
  private final Map<String, String> classHierarchy;
  private final Map<String, Boolean> junit3TestsCache;
  private final Map<String, Map<String, Void>> junit4TestsCache;

  public TestDetector() {
    classHierarchy = MapBuilder.<String, String>builder().initialCapacity(1 << 10).build();
    junit3TestsCache = MapBuilder.<String, Boolean>builder().initialCapacity(1 << 10).build();
    junit4TestsCache =
        MapBuilder.<String, Map<String, Void>>builder().initialCapacity(1 << 10).build();
  }

  public void registerClass(final String superName, final String className) {
    synchronized (classHierarchy) {
      classHierarchy.getOrPut(className, () -> superName);
    }
  }

  public boolean isJUnit3TestMethod(
      final String superName,
      final int access,
      final String className,
      final String methodName,
      final String description) {
    return isJUnit3TestClass(superName, className)
        && ((methodName.startsWith("test") && (access & Opcodes.ACC_PUBLIC) != 0)
            || (methodName.equals("runTest") && description.equals("()V")));
  }

  public boolean isJUnit4TestMethod(
      final String className, final String methodName, final String description) {
    synchronized (junit4TestsCache) {
      final Map<String, Void> classMethods = junit4TestsCache.get(className);
      if (classMethods != null) {
        return classMethods.contains(methodKey(methodName, description));
      }

      synchronized (classHierarchy) {
        String parent = classHierarchy.get(className);
        while (parent != null) {
          final Map<String, Void> parentMethods = junit4TestsCache.get(parent);
          if (parentMethods != null) {
            return parentMethods.contains(methodKey(methodName, description));
          }
          parent = classHierarchy.get(parent);
        }
      }

      return false;
    }
  }

  public void registerJUint4TestMethod(
      final String className, final String methodName, final String description) {
    synchronized (junit4TestsCache) {
      final Map<String, Void> classTestMethods =
          junit4TestsCache.getOrPut(
              className, () -> MapBuilder.<String, Void>builder().initialCapacity(8).build());

      classTestMethods.getOrPut(methodKey(methodName, description), () -> null);
    }
  }

  private boolean isJUnit3TestClass(final String superName, final String className) {
    synchronized (junit3TestsCache) {
      final Boolean isTest = junit3TestsCache.get(className);
      if (isTest != null) return isTest;

      final boolean isTestCaseSubclass = isSubclass(className, "junit/framework/TestCase");
      return junit3TestsCache.getOrPut(className, () -> isTestCaseSubclass);
    }
  }

  private boolean isSubclass(final String className, final String superClass) {
    synchronized (classHierarchy) {
      String parent = classHierarchy.get(className);
      while (parent != null) {
        if (parent.equals(superClass)) return true;
        parent = classHierarchy.get(parent);
      }
    }

    return false;
  }

  private String methodKey(final String name, final String description) {
    return name + "#" + description;
  }
}
