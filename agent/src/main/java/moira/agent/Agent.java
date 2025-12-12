package moira.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class Agent {
  private static String DEFAULT_PROFILER = "moira/profiler/NullProfiler";

  private Agent() {}

  public static void premain(final String args, final Instrumentation instrumentation)
      throws UnmodifiableClassException {
    final String profilerName = System.getProperty("moira.profiler.name");

    String profiler = DEFAULT_PROFILER;
    if (profilerName != null && !profilerName.isEmpty())
      profiler = "moira/profiler/" + profilerName;

    instrumentation.addTransformer(new Transformer(profiler), true);

    if (!instrumentation.isRetransformClassesSupported()) return;

    Class<?>[] classes = instrumentation.getAllLoadedClasses();
    for (int i = 0; i < classes.length; ++i)
      if (instrumentation.isModifiableClass(classes[i]))
        instrumentation.retransformClasses(classes[i]);
  }
}
