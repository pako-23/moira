package ch.usi.inf.agent;

import ch.usi.inf.profiler.Map;
import ch.usi.inf.profiler.MapBuilder;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Agent {
  public static String PROFILER = "ch/usi/inf/profiler/NullProfiler";
  public static Map<String, Boolean> testsFilter = null;
  private static String fileName = "conflicts";

  public static void premain(String agentArgs, Instrumentation inst) {
    String profilerName = System.getProperty("agent.profiler.name");
    String fileName = System.getProperty("agent.profiler.filename");
    String filterFileName = System.getProperty("agent.profiler.filter.filename");

    if (profilerName != null && !profilerName.isEmpty())
      PROFILER = "ch/usi/inf/profiler/" + profilerName;
    if (fileName != null && !fileName.isEmpty()) Agent.fileName = fileName;
    if (filterFileName != null && filterFileName.isEmpty()) {
      initializeTestsFilter(filterFileName);
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

  private static void initializeTestsFilter(final String fileName) {
    testsFilter = MapBuilder.<String, Boolean>builder().build();

    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
      lines.forEach(Agent::registerTestFilter);
    } catch (IOException e) {
      System.err.println("Warning: failed to read tests filter: " + e.getMessage());
      testsFilter = null;
    }
  }

  private static void registerTestFilter(final String line) {
    final String[] tests = line.split(" ");
    for (final String test : tests) testsFilter.getOrPut(test, () -> true);
  }
}
