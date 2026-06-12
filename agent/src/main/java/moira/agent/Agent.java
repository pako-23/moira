package moira.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class Agent {
  private Agent() {}

  public static void premain(final String args, final Instrumentation instrumentation)
      throws UnmodifiableClassException {
    instrumentation.addTransformer(new Transformer(), true);

    if (!instrumentation.isRetransformClassesSupported()) return;

    Class<?>[] classes = instrumentation.getAllLoadedClasses();
    for (int i = 0; i < classes.length; ++i)
      if (instrumentation.isModifiableClass(classes[i]))
        instrumentation.retransformClasses(classes[i]);
  }
}
