package ch.usi.inf.profiler;

import java.io.FileNotFoundException;

public class ObjectProfiler {
  private static ThreadSuspension suspension;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, ReadWriteSet> arrayMapping;
  private static Map<Object, ReadWriteSet> objectMapping;
  private static volatile int runningTest = -1;

  static {
    setup();
  }

  private ObjectProfiler() {}

  public static void setup() {
    suspension = new ThreadSuspension();
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
  }

  public static void suspend() {
    suspension.suspend();
  }

  public static void resume() {
    suspension.resume();
  }

  public static void dump(final String fileName) throws FileNotFoundException {
    mappingDump(staticMapping);
    mappingDump(arrayMapping);
    mappingDump(objectMapping);
    dump.dump(fileName);
  }

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (runningTest < 0) return;
    if (suspension.isSuspended()) return;

    synchronized (staticMapping) {
      suspend();
      ReadWriteSet set = staticMapping.getOrPut(field, () -> new ReadWriteSet());
      set.update(runningTest, event);
      resume();
    }
  }

  private static void objectEvent(final Object object, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (runningTest < 0) return;
    if (object == null) return;
    if (suspension.isSuspended()) return;

    synchronized (objectMapping) {
      suspend();
      ReadWriteSet set = objectMapping.getOrPut(object, () -> new ReadWriteSet());
      set.update(runningTest, event);
      resume();
    }
  }

  private static void arrayEvent(final Object array, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (suspension.isSuspended()) return;

    synchronized (arrayMapping) {
      suspend();
      ReadWriteSet set = arrayMapping.getOrPut(array, () -> new ReadWriteSet());
      set.update(runningTest, event);
      resume();
    }
  }

  private static void mappingDump(final Map<?, ReadWriteSet> mapping) {
    Map.Iterator<?, ReadWriteSet> it = mapping.iterator();

    while (it.hasNext()) {
      dump.computeConflicts(it.value());
      it.next();
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
