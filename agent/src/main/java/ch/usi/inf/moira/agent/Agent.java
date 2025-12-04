package ch.usi.inf.moira.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class Agent {
  public static String PROFILER = "ch/usi/inf/moira/profiler/NullProfiler";

  public static void premain(String args, Instrumentation instrumentation)
      throws UnmodifiableClassException {
    String profilerName = System.getProperty("moira.profiler.name");

    if (profilerName != null && !profilerName.isEmpty())
      PROFILER = "ch/usi/inf/moira/profiler/" + profilerName;

    instrumentation.addTransformer(new Transformer(), true);

    if (!instrumentation.isRetransformClassesSupported()) return;

    Class<?>[] classes = instrumentation.getAllLoadedClasses();
    for (int i = 0; i < classes.length; ++i)
      if (instrumentation.isModifiableClass(classes[i]))
        instrumentation.retransformClasses(classes[i]);
  }
}
