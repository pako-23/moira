package moira.agent;

public class ManglerConfig {
  private static final String[] DEFAULT_FILTER_PREFIXES = {
    "java/io/",
    "java/lang/",
    "java/net/",
    "java/nio/",
    "java/text/",
    "java/util/concurrent/locks/",
    "jdk/",
    "junit/",
    "moira/",
    "org/junit/",
    "org/objectweb/asm/",
    "sun/",
  };

  private static String DEFAULT_PROFILER = "moira/profiler/NullProfiler";

  private static String[] DEFAULT_SUSPEND_PREFIXES = {
    "java/lang/ClassLoader",
    "java/lang/invoke/MethodHandleNatives",
    "java/net/URLClassLoader",
    "java/security/SecureClassLoader",
  };

  private final String profiler;
  private final String[] filterPrefixes;
  private final String[] suspendPrefixes;

  public ManglerConfig() {
    this.profiler = detectProfiler();
    this.filterPrefixes = detectFilterPrefixes();
    this.suspendPrefixes = detectSuspendPrefixes();
  }

  public String getProfiler() {
    return profiler;
  }

  public boolean isSuspended(final String className) {
    return prefixMatch(className, suspendPrefixes);
  }

  public boolean shouldMangle(final String className) {
    return !prefixMatch(className, filterPrefixes);
  }

  private String detectProfiler() {
    final String profilerName = System.getProperty("moira.profiler.name");

    String profiler = DEFAULT_PROFILER;
    if (profilerName != null && !profilerName.isEmpty())
      profiler = "moira/profiler/" + profilerName;

    return profiler;
  }

  private String[] detectFilterPrefixes() {
    final String filter = System.getProperty("moira.agent.filter");
    if (filter == null || filter.isEmpty()) return DEFAULT_FILTER_PREFIXES;
    else return joinArrays(DEFAULT_FILTER_PREFIXES, filter.split(","));
  }

  private String[] detectSuspendPrefixes() {
    final String suspend = System.getProperty("moira.agent.suspend");
    if (suspend == null) return DEFAULT_SUSPEND_PREFIXES;
    else if (suspend.isEmpty()) return new String[0];
    else return suspend.split(",");
  }

  private String[] joinArrays(final String[] a, final String[] b) {
    int i = 0;
    final String[] result = new String[a.length + b.length];

    for (; i < a.length; ++i) result[i] = a[i];
    for (int j = 0; j < b.length; ++j, ++i) result[i] = b[j];

    return result;
  }

  private boolean prefixMatch(final String item, final String[] prefixes) {
    for (final String prefix : prefixes) {
      if (item.startsWith(prefix)) return true;
    }

    return false;
  }
}
