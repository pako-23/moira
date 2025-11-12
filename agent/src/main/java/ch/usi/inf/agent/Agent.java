package ch.usi.inf.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class Agent {
  public static String PROFILER = "ch/usi/inf/profiler/NullProfiler";
  private static String fileName = "conflicts";

  public static void premain(String agentArgs, Instrumentation inst) {
    String profilerName = System.getProperty("agent.profiler.name");
    String fileName = System.getProperty("agent.profiler.filename");
    String filterFileName = System.getProperty("agent.profiler.filter.filename");

    if (profilerName != null && !profilerName.isEmpty())
      PROFILER = "ch/usi/inf/profiler/" + profilerName;
    if (fileName != null && !fileName.isEmpty()) Agent.fileName = fileName;
    if (filterFileName != null && !filterFileName.isEmpty()) {
      TestCaseMangler.registerTestsFilter(filterFileName);
    }

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                try {
                  final Class<?> profilerClass = Class.forName(PROFILER.replace("/", "."));
                  final Method dump = profilerClass.getMethod("dump", String.class);
                  dump.invoke(null, Agent.fileName);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });

    try {
      inst.addTransformer(new Transformer(), true);

      if (!inst.isRetransformClassesSupported()) return;

      Class<?>[] classes = inst.getAllLoadedClasses();
      for (int i = 0; i < classes.length; ++i)
        if (inst.isModifiableClass(classes[i])) inst.retransformClasses(classes[i]);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
