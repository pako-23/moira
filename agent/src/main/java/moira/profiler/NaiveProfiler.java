package moira.profiler;

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import moira.collect.ArrayMap;
import moira.collect.Map;
import moira.collect.MapBuilder;

public final class NaiveProfiler {
  private static volatile int enabled;
  private static ThreadLocal<Integer> suspend;
  private static int runningTest;
  private static ProfilerDump dump;
  private static Map<String, Event> staticMapping;
  private static Map<Object, Map<String, Event>> objectMapping;
  private static Map<Object, Map<Integer, Event>> arrayMapping;
  private static TestSnapshot snapshots;

  private static class Event {
    private byte mask;

    private Event() {
      mask = 0;
    }

    public void update(final byte event) {
      if (mask == 0 && event == ReadWriteSet.READ) mask |= ReadWriteSet.READ_BEFORE_WRITE;
      mask |= event;
    }

    public boolean write() {
      return (mask & ReadWriteSet.WRITE) != 0;
    }

    public boolean readBeforeWrite() {
      return (mask & ReadWriteSet.READ_BEFORE_WRITE) != 0;
    }
  }

  private static class TestSnapshot {
    private final int test;
    private final Map<String, Event> staticMapping;
    private final Map<Object, Map<String, Event>> objectMapping;
    private final Map<Object, Map<Integer, Event>> arrayMapping;
    private TestSnapshot next;

    private TestSnapshot(final int test) {
      this.test = test;
      this.staticMapping = NaiveProfiler.staticMapping;
      this.objectMapping = NaiveProfiler.objectMapping;
      this.arrayMapping = NaiveProfiler.arrayMapping;
      this.next = null;
    }

    public void setNext(final TestSnapshot next) {
      this.next = next;
    }

    public TestSnapshot getNext() {
      return next;
    }

    public int getTest() {
      return test;
    }

    private static <K> boolean oneLevelMapDependencies(
        final Map<K, Event> from, final Map<K, Event> to) {
      final Map.Iterator<K, Event> it = to.iterator();
      for (; it.hasNext(); it.next()) {
        if (!it.value().write()) continue;
        final Event value = from.get(it.key());
        if (value != null && value.readBeforeWrite()) return true;
      }
      return false;
    }

    private static <F, S> boolean twoLevelMapDependencies(
        final Map<F, Map<S, Event>> from, final Map<F, Map<S, Event>> to) {
      final Map.Iterator<F, Map<S, Event>> it = to.iterator();
      for (; it.hasNext(); it.next()) {
        final Map<S, Event> mapping = from.get(it.key());
        if (mapping == null) continue;

        if (TestSnapshot.<S>oneLevelMapDependencies(mapping, it.value())) return true;
      }
      return false;
    }

    private boolean staticDependencies(final TestSnapshot other) {
      return TestSnapshot.<String>oneLevelMapDependencies(staticMapping, other.staticMapping);
    }

    private boolean arrayDependencies(final TestSnapshot other) {
      return TestSnapshot.<Object, Integer>twoLevelMapDependencies(
          arrayMapping, other.arrayMapping);
    }

    private boolean objectDependencies(final TestSnapshot other) {
      return TestSnapshot.<Object, String>twoLevelMapDependencies(
          objectMapping, other.objectMapping);
    }

    public boolean depends(final TestSnapshot other) {
      return staticDependencies(other) || objectDependencies(other) || arrayDependencies(other);
    }
  }

  static {
    setup();
  }

  private NaiveProfiler() {}

  public static void setup() {
    runningTest = -1;
    enabled = 0;
    suspend = ThreadLocal.withInitial(() -> 0);
    dump = new ProfilerDump();
    staticMapping = null;
    objectMapping = null;
    arrayMapping = null;
    snapshots = null;
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

  public static void dump(final String fileName) throws FileNotFoundException {
    for (TestSnapshot first = snapshots; first != null; first = first.getNext()) {
      for (TestSnapshot second = first.getNext(); second != null; second = second.getNext()) {
        if (first.depends(second)) dump.registerDependency(first.getTest(), second.getTest());
        if (second.depends(first)) dump.registerDependency(second.getTest(), first.getTest());
      }
    }

    dump.dump(fileName);
  }

  private static void staticFieldEvent(final String field, final byte event) {
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (staticMapping) {
      staticMapping.getOrPut(field, () -> new Event()).update(event);
      resume();
    }
  }

  private static void objectEvent(final Object object, final String field, final byte event) {
    if (object == null) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (objectMapping) {
      objectMapping
          .getOrPut(object, () -> MapBuilder.<String, Event>builder().initialCapacity(4).build())
          .getOrPut(field, () -> new Event())
          .update(event);
      resume();
    }
  }

  private static void arrayEvent(final Object array, final int index, final byte event) {
    if (array == null) return;
    if (enabled == 0) return;
    if (suspendedOrSuspend()) return;

    synchronized (arrayMapping) {
      arrayMapping
          .getOrPut(array, () -> new ArrayMap<>(Array.getLength(array)))
          .getOrPut(index, () -> new Event())
          .update(event);
      resume();
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
    staticMapping = MapBuilder.<String, Event>builder().initialCapacity(1 << 8).build();
    objectMapping =
        MapBuilder.<Object, Map<String, Event>>builder().initialCapacity(1 << 8).build();
    arrayMapping =
        MapBuilder.<Object, Map<Integer, Event>>builder().initialCapacity(1 << 8).build();
    runningTest = dump.registerTest(test);
  }

  public static void exitTestMethod() {
    final TestSnapshot node = new TestSnapshot(runningTest);
    node.setNext(snapshots);
    snapshots = node;
    runningTest = -1;
  }
}
