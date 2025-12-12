package moira;

import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class Moira {
  private static final String DEFAULT_PROFILER = "moira.profiler.NullProfiler";

  private final String profiler;

  public Moira() {
    final String profilerName = System.getProperty("moira.profiler.name");
    if (profilerName != null && !profilerName.isEmpty())
      profiler = "moira.profiler." + profilerName;
    else profiler = DEFAULT_PROFILER;
  }

  public int run(final String... classes) {
    try {
      final Class<?>[] testClasses = new Class<?>[classes.length];

      for (int i = 0; i < classes.length; ++i) {
        try {
          testClasses[i] = Class.forName(classes[i]);
        } catch (final ClassNotFoundException e) {
          System.err.println("Could not find class[" + classes[i] + "]");
          throw e;
        }
      }

      final Request request = Request.classes(testClasses);
      final JUnitCore junit = new JUnitCore();
      final ProfilerProxy profilerProxy = new ProfilerProxy(profiler);

      junit.addListener(new TextListener(new RealSystem()));
      junit.addListener(new MoiraListener(profilerProxy));

      System.out.println("JUnit version " + junit.getVersion());
      final Result result = junit.run(request);

      if (!result.wasSuccessful()) return 1;

      profilerProxy.dump();
      return 0;
    } catch (final Exception e) {
      e.printStackTrace();
      return 1;
    }
  }

  public static void main(final String[] args) {
    System.exit(new Moira().run(args));
  }
}
