package moira.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class Agent {
  private Agent() {}

  public static void premain(final String args, final Instrumentation instrumentation)
      throws UnmodifiableClassException, ClassNotFoundException {
    final ManglerConfig config = new ManglerConfig();
    final Class<?> profilerClass = Class.forName(config.getProfiler().replace("/", "."));

    instrumentation.addTransformer(new Transformer(config), true);

    if (!instrumentation.isRetransformClassesSupported()) return;

    final Class<?>[] classes = instrumentation.getAllLoadedClasses();
    for (int i = 0; i < classes.length; ++i) {

      if (instrumentation.isModifiableClass(classes[i])
          && !classes[i].equals(profilerClass)
          && !classes[i].getName().startsWith("java.lang.invoke.LambdaForm")) {
        instrumentation.retransformClasses(classes[i]);
      }
    }
  }
}
