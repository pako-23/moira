package moira.util.service;

public enum Profiler {
  ONLINE("online", "OnlineProfiler"),
  NAIVE("naive", "NaiveProfiler"),
  OBJECT("object", "ObjectProfiler"),
  TARGET_PAIRS("target-pairs", "TargetPairsProfiler"),
  NULL("null", "NullProfiler");

  private final String name;
  private final String profilerClass;

  private Profiler(final String name, final String profilerClass) {
    this.name = name;
    this.profilerClass = profilerClass;
  }

  @Override
  public String toString() {
    return name;
  }

  public static Profiler fromString(final String value) {
    for (final Profiler profiler : values()) if (profiler.name.equals(value)) return profiler;

    return null;
  }

  public String getProfilerClass() {
    return profilerClass;
  }
}
