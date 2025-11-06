package ch.usi.inf.profiler;

import java.io.FileNotFoundException;

public class ObjectProfiler {
  private static ThreadLocal<Boolean> suspended;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, ReadWriteSet> arrayMapping;
  private static Map<Object, ReadWriteSet> objectMapping;
  private static volatile int runningTest = -1;

  static {
    suspended = new ThreadLocal<>();
    dump = new ProfilerDump();
    staticMapping = MapBuilder.<String, ReadWriteSet>builder().initialCapacity(64).build();
    arrayMapping =
        MapBuilder.<Object, ReadWriteSet>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .keyDeletionCallback(set -> dump.computeConflicts(set))
            .hashFunction(object -> System.identityHashCode(object))
            .equivalence((first, second) -> first == second)
            .build();
    objectMapping =
        MapBuilder.<Object, ReadWriteSet>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .keyDeletionCallback(set -> dump.computeConflicts(set))
            .hashFunction(object -> System.identityHashCode(object))
            .equivalence((first, second) -> first == second)
            .build();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                mappingDump(staticMapping);
                mappingDump(arrayMapping);
                mappingDump(objectMapping);
                try {
                  dump.dump("conflicts");
                } catch (FileNotFoundException e) {
                  System.err.println("Failed to write profiler dump: " + e.getMessage());
                }
              }
            });
  }

  private ObjectProfiler() {}

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (runningTest < 0) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (staticMapping) {
      suspended.set(true);
      ReadWriteSet set = staticMapping.getOrPut(field, () -> new ReadWriteSet());
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  private static void mappingDump(final Map<?, ReadWriteSet> mapping) {
    Map.Iterator<?, ReadWriteSet> it = mapping.iterator();

    while (it.hasNext()) {
      dump.computeConflicts(it.value());
      it.next();
    }
  }

  private static void objectEvent(final Object object, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (runningTest < 0) return;
    if (object == null) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (objectMapping) {
      suspended.set(true);
      ReadWriteSet set = objectMapping.getOrPut(object, () -> new ReadWriteSet());
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  private static void arrayEvent(final Object array, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (arrayMapping) {
      suspended.set(true);
      ReadWriteSet set = arrayMapping.getOrPut(array, () -> new ReadWriteSet());
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  public static void writeStaticField(final String field) {
    staticFieldEvent(field, ReadWriteSet.WRITE);
  }

  public static void writeArrayElement(final Object array, final int index) {
    arrayEvent(array, ReadWriteSet.WRITE);
  }

  public static void writeObjectField(final Object object, final String field) {
    objectEvent(object, ReadWriteSet.WRITE);
  }

  public static void readStaticField(final String field) {
    staticFieldEvent(field, ReadWriteSet.READ);
  }

  public static void readArrayElement(final Object array, final int index) {
    arrayEvent(array, ReadWriteSet.READ);
  }

  public static void readObjectField(final Object object, final String field) {
    objectEvent(object, ReadWriteSet.READ);
  }

  public static void enterTestMethod(final String test) {
    runningTest = dump.registerTest(test);
  }

  public static void exitTestMethod() {
    runningTest = -1;
  }
}
