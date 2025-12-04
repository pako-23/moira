package ch.usi.inf.moira;

import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

public class Moira {
  public static void main(final String[] args)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    final Class<?>[] classes = new Class<?>[args.length];

    for (int i = 0; i < args.length; ++i) {
      try {
        classes[i] = Class.forName(args[i]);
      } catch (final ClassNotFoundException e) {
        e.printStackTrace();
        System.err.println("Could not find class[" + args[i] + "]");
        System.exit(1);
      }
    }

    final Request request = Request.classes(classes);
    final JUnitCore junit = new JUnitCore();
    String profiler = "ch.usi.inf.moira.profiler.NullProfiler";
    final String profilerName = System.getProperty("moira.profiler.name");
    if (profilerName != null && !profilerName.isEmpty())
      profiler = "ch.usi.inf.moira.profiler." + profilerName;
    final ProfilerProxy profilerProxy = new ProfilerProxy(profiler);

    junit.addListener(new TextListener(new RealSystem()));
    junit.addListener(new MoiraListener(profilerProxy));

    System.out.println("JUnit version " + junit.getVersion());
    final Result result = junit.run(request);

    if (!result.wasSuccessful()) System.exit(1);

    profilerProxy.dump();
    System.exit(0);
  }
}
