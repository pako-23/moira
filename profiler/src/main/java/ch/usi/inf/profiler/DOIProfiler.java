package ch.usi.inf.profiler;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;

public class DOIProfiler {
  private static ThreadLocal<Boolean> suspended;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, Map<Integer, ReadWriteSet>> arrayMapping;
  private static Map<Object, Map<String, ReadWriteSet>> objectMapping;
  private static volatile int runningTest = -1;

  static {
    suspended = new ThreadLocal<>();
    dump = new ProfilerDump();
    staticMapping = MapBuilder.<String, ReadWriteSet>builder().initialCapacity(64).build();
    arrayMapping =
        MapBuilder.<Object, Map<Integer, ReadWriteSet>>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .keyDeletionCallback(DOIProfiler::fieldMappingDump)
            .hashFunction(object -> System.identityHashCode(object))
            .equivalence((first, second) -> first == second)
            .build();
    objectMapping =
        MapBuilder.<Object, Map<String, ReadWriteSet>>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .keyDeletionCallback(DOIProfiler::fieldMappingDump)
            .hashFunction(object -> System.identityHashCode(object))
            .equivalence((first, second) -> first == second)
            .build();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                fieldMappingDump(staticMapping);
                arrayMappingDump();
                objectMappingDump();

                try {
                  dump.dump("conflicts");
                } catch (FileNotFoundException e) {
                  System.err.println("Failed to write profiler dump: " + e.getMessage());
                }
              }
            });
  }

  private DOIProfiler() {}

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (runningTest < 0) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (staticMapping) {
      suspended.set(true);
      ReadWriteSet set = staticMapping.getOrPut(field, () -> new ReadWriteSet(runningTest));
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  private static void arrayMappingDump() {
    Map.Iterator<?, Map<Integer, ReadWriteSet>> it = arrayMapping.iterator();

    while (it.hasNext()) {
      fieldMappingDump(it.value());
      it.next();
    }
  }

  private static void objectMappingDump() {
    Map.Iterator<?, Map<String, ReadWriteSet>> it = objectMapping.iterator();

    while (it.hasNext()) {
      fieldMappingDump(it.value());
      it.next();
    }
  }

  private static void fieldMappingDump(final Map<?, ReadWriteSet> mapping) {
    Map.Iterator<?, ReadWriteSet> it = mapping.iterator();

    while (it.hasNext()) {
      dump.computeConflicts(it.value());
      it.next();
    }
  }

  private static void objectEvent(final Object object, final String field, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (runningTest < 0) return;
    if (object == null) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (objectMapping) {
      suspended.set(true);
      Map<String, ReadWriteSet> fieldMapping =
          objectMapping.getOrPut(
              object, () -> MapBuilder.<String, ReadWriteSet>builder().initialCapacity(4).build());
      ReadWriteSet set = fieldMapping.getOrPut(field, () -> new ReadWriteSet(runningTest));
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  private static void arrayEvent(final Object array, final int index, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (suspended.get() != null && suspended.get()) return;

    synchronized (arrayMapping) {
      suspended.set(true);
      Map<Integer, ReadWriteSet> mapping =
          arrayMapping.getOrPut(array, () -> new ArrayMap<>(Array.getLength(array)));
      ReadWriteSet set = mapping.getOrPut(index, () -> new ReadWriteSet(runningTest));
      set.update(runningTest, event);
      suspended.set(false);
    }
  }

  public static void writeStaticField(final String field) {
    staticFieldEvent(field, ReadWriteSet.WRITE);
  }

  public static void writeArrayElement(final Object array, final int index) {
    arrayEvent(array, index, ReadWriteSet.WRITE);
  }

  public static void writeObjectField(final Object object, final String field) {
    objectEvent(object, field, ReadWriteSet.WRITE);
  }

  public static void readStaticField(final String field) {
    staticFieldEvent(field, ReadWriteSet.READ);
  }

  public static void readArrayElement(final Object array, final int index) {
    arrayEvent(array, index, ReadWriteSet.READ);
  }

  public static void readObjectField(final Object object, final String field) {
    objectEvent(object, field, ReadWriteSet.READ);
  }

  public static void enterTestMethod(final String test) {
    runningTest = dump.registerTest(test);
  }

  public static void exitTestMethod() {
    runningTest = -1;
  }
}
