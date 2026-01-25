package moira.profiler;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import moira.collect.ArrayMap;
import moira.collect.Map;
import moira.collect.MapBuilder;

public final class DOIProfiler {
  private static volatile int runningTest;
  private static volatile int enabled;
  private static ThreadLocal<Integer> suspend;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, Map<Integer, ReadWriteSet>> arrayMapping;
  private static Map<Object, Map<String, ReadWriteSet>> objectMapping;

  static {
    setup();
  }

  private DOIProfiler() {}

  public static void setup() {
    runningTest = -1;
    enabled = 0;
    suspend = ThreadLocal.withInitial(() -> 0);
    dump = new ProfilerDump();
    staticMapping = MapBuilder.<String, ReadWriteSet>builder().initialCapacity(1 << 10).build();
    arrayMapping =
        MapBuilder.<Object, Map<Integer, ReadWriteSet>>builder()
            .initialCapacity(1 << 11)
            .weakKeys()
            .keyDeletionCallback(DOIProfiler::fieldMappingDump)
            .hashFunction(System::identityHashCode)
            .build();
    objectMapping =
        MapBuilder.<Object, Map<String, ReadWriteSet>>builder()
            .initialCapacity(1 << 11)
            .weakKeys()
            .keyDeletionCallback(DOIProfiler::fieldMappingDump)
            .hashFunction(System::identityHashCode)
            .build();
  }

  public static void dump(final String fileName) throws FileNotFoundException {
    fieldMappingDump(staticMapping);
    arrayMappingDump();
    objectMappingDump();
    dump.dump(fileName);
  }

  public static void suspend() {
    suspend.set(suspend.get() + 1);
  }

  public static void resume() {
    suspend.set(suspend.get() - 1);
  }

  public static synchronized void enable() {
    ++enabled;
  }

  public static synchronized void disable() {
    --enabled;
  }

  private static boolean suspendedOrSuspend() {
    final Integer value = suspend.get();
    if (value != 0) return true;
    suspend.set(1);
    return false;
  }

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (staticMapping) {
      staticMapping.getOrPut(field, () -> new ReadWriteSet()).update(runningTest, event);
    }
    resume();
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
    if (object == null) return;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (objectMapping) {
      objectMapping
          .getOrPut(
              object, () -> MapBuilder.<String, ReadWriteSet>builder().initialCapacity(4).build())
          .getOrPut(field, () -> new ReadWriteSet())
          .update(runningTest, event);
    }
    resume();
  }

  private static void arrayEvent(final Object array, final int index, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (arrayMapping) {
      arrayMapping
          .getOrPut(array, () -> new ArrayMap<>(Array.getLength(array)))
          .getOrPut(index, () -> new ReadWriteSet())
          .update(runningTest, event);
    }
    resume();
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
