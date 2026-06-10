package moira.profiler;

import java.io.FileNotFoundException;
import moira.collect.Map;
import moira.collect.MapBuilder;

public final class ObjectProfiler {
  private static volatile int runningTest;
  private static volatile int enabled;
  private static ThreadLocal<Integer> suspend;
  private static DataFlows dataFlows;
  private static Map<String, ReadWriteSet> staticMapping;
  private static Map<Object, ReadWriteSet> arrayMapping;
  private static Map<Object, ReadWriteSet> objectMapping;

  static {
    setup();
  }

  private ObjectProfiler() {}

  public static void setup() {
    runningTest = -1;
    enabled = 0;
    suspend = ThreadLocal.withInitial(() -> 0);
    dataFlows = new DataFlows();
    staticMapping = MapBuilder.<String, ReadWriteSet>builder().initialCapacity(1 << 10).build();
    arrayMapping =
        MapBuilder.<Object, ReadWriteSet>builder()
            .initialCapacity(1 << 11)
            .weakKeys()
            .keyDeletionCallback(ObjectProfiler::computeDataFlows)
            .hashFunction(System::identityHashCode)
            .build();
    objectMapping =
        MapBuilder.<Object, ReadWriteSet>builder()
            .initialCapacity(1 << 24)
            .weakKeys()
            .keyDeletionCallback(ObjectProfiler::computeDataFlows)
            .hashFunction(System::identityHashCode)
            .build();
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

  private static void computeDataFlows(final ReadWriteSet set) {
    dataFlows.update(set);
  }

  public static void dump(final String fileName) throws FileNotFoundException {
    mappingDump(staticMapping);
    mappingDump(arrayMapping);
    mappingDump(objectMapping);
    dataFlows.dump(fileName);
  }

  private static void staticFieldEvent(final String field, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (staticMapping) {
      staticMapping.getOrPut(field, () -> new ReadWriteSet()).update(runningTest, event);
    }
    resume();
  }

  private static void objectEvent(final Object object, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (object == null) return;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (objectMapping) {
      objectMapping.getOrPut(object, () -> new ReadWriteSet()).update(runningTest, event);
    }
    resume();
  }

  private static void arrayEvent(final Object array, final byte event) {
    int runningTest = ObjectProfiler.runningTest;
    if (array == null) return;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (arrayMapping) {
      arrayMapping.getOrPut(array, () -> new ReadWriteSet()).update(runningTest, event);
    }
    resume();
  }

  private static void mappingDump(final Map<?, ReadWriteSet> mapping) {
    Map.Iterator<?, ReadWriteSet> it = mapping.iterator();

    while (it.hasNext()) {
      dataFlows.update(it.value());
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
    runningTest = dataFlows.registerTest(test);
  }

  public static void exitTestMethod() {
    runningTest = -1;
  }
}
