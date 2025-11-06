package ch.usi.inf.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class Agent {
  public static String PROFILER = "ch/usi/inf/profiler/NullProfiler";

  public static void premain(String agentArgs, Instrumentation inst) {
    if (agentArgs != null && !agentArgs.isEmpty()) PROFILER = "ch/usi/inf/profiler/" + agentArgs;

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                try {
                  final Class<?> profilerClass = Class.forName(PROFILER.replace("/", "."));
                  final Method dump = profilerClass.getMethod("dump", String.class);
                  dump.invoke(null, "conflicts");
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
