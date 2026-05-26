package moira.profiler;

import java.io.FileNotFoundException;
import moira.collect.Map;
import moira.collect.MapBuilder;

public final class TargetedPairsProfiler {
  private static volatile int runningTest;
  private static volatile int enabled;
  private static ThreadLocal<Integer> suspend;
  private static ProfilerDump dump;
  private static Map<String, ReadWriteSet> mapping;

  static {
    setup();
  }

  private TargetedPairsProfiler() {}

  public static void setup() {
    runningTest = -1;
    enabled = 0;
    suspend = ThreadLocal.withInitial(() -> 0);
    dump = new ProfilerDump();
    mapping = MapBuilder.<String, ReadWriteSet>builder().initialCapacity(1 << 10).build();
  }

  private static void staticFieldEvent(final String field) {
    int runningTest = TargetedPairsProfiler.runningTest;
    if (runningTest < 0) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (mapping) {
      mapping
          .getOrPut(field, () -> new ReadWriteSet())
          .update(runningTest, (byte) (ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.WRITE));
    }
    suspend.set(suspend.get() - 1);
  }

  private static boolean suspendedOrSuspend() {
    final Integer value = suspend.get();
    if (value != 0) return true;
    suspend.set(1);
    return false;
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

  public static void writeStaticField(final String field) {
    staticFieldEvent(field);
  }

  public static void writeArrayElement(final Object array, final int index) {}

  public static void writeObjectField(final Object object, final String field) {}

  public static void readStaticField(final String field) {
    staticFieldEvent(field);
  }

  public static void readArrayElement(final Object array, final int index) {}

  public static void readObjectField(final Object object, final String field) {}

  public static void enterTestMethod(final String test) {
    runningTest = dump.registerTest(test);
  }

  public static void exitTestMethod() {
    runningTest = -1;
  }

  public static void dump(final String fileName) throws FileNotFoundException {
    Map.Iterator<String, ReadWriteSet> it = mapping.iterator();

    while (it.hasNext()) {
      System.out.println(it.key());
      dump.computeConflicts(it.value());
      it.next();
    }

    dump.dump(fileName);
  }
}
