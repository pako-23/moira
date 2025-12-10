package ch.usi.inf.moira.profiler;

import ch.usi.inf.moira.collect.ArrayMap;
import ch.usi.inf.moira.collect.Map;
import ch.usi.inf.moira.collect.MapBuilder;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;

public final class DOIProfiler {
  private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

  private static ThreadSuspension suspension;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, Map<Integer, ReadWriteSet>> arrayMapping;
  private static Map<Object, Map<String, ReadWriteSet>> objectMapping;
  private static volatile int runningTest = -1;
  private static volatile int enabled = 0;

  static {
    setup();
  }

  private DOIProfiler() {}

  public static void setup() {
    suspension = new ThreadSuspension();
    dump = new ProfilerDump();
    staticMapping =
        MapBuilder.<String, ReadWriteSet>builder()
            .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
            .initialCapacity(64)
            .build();
    arrayMapping =
        MapBuilder.<Object, Map<Integer, ReadWriteSet>>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
            .keyDeletionCallback(DOIProfiler::fieldMappingDump)
            .hashFunction(System::identityHashCode)
            .build();
    objectMapping =
        MapBuilder.<Object, Map<String, ReadWriteSet>>builder()
            .initialCapacity(1 << 10)
            .weakKeys()
            .concurrencyLevel(DEFAULT_CONCURRENCY_LEVEL)
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
    suspension.suspend();
  }

  public static void resume() {
    suspension.resume();
  }

  public static void enable() {
    ++enabled;
  }

  public static void disable() {
    --enabled;
  }

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspension.suspend()) return;

    ReadWriteSet set = staticMapping.getOrPut(field, () -> new ReadWriteSet());
    synchronized (set) {
      set.update(runningTest, event);
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
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (object == null) return;
    if (suspension.suspend()) return;

    final Map<String, ReadWriteSet> fieldMapping =
        objectMapping.getOrPut(
            object, () -> MapBuilder.<String, ReadWriteSet>builder().initialCapacity(4).build());
    final ReadWriteSet set = fieldMapping.getOrPut(field, () -> new ReadWriteSet());
    synchronized (set) {
      set.update(runningTest, event);
    }
    resume();
  }

  private static void arrayEvent(final Object array, final int index, final byte event) {
    int runningTest = DOIProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspension.suspend()) return;

    final Map<Integer, ReadWriteSet> mapping =
        arrayMapping.getOrPut(array, () -> new ArrayMap<>(Array.getLength(array)));
    final ReadWriteSet set = mapping.getOrPut(index, () -> new ReadWriteSet());
    synchronized (set) {
      set.update(runningTest, event);
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
