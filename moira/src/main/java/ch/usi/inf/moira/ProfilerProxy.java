package ch.usi.inf.moira;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ProfilerProxy {
  private final MethodHandle enterTestMethod;
  private final MethodHandle exitTestMethod;
  private final MethodHandle dump;

  public ProfilerProxy(final String profiler)
      throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
    final Class<?> profilerClass = Class.forName(profiler);
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    enterTestMethod =
        lookup.findStatic(
            profilerClass, "enterTestMethod", MethodType.methodType(void.class, String.class));
    exitTestMethod =
        lookup.findStatic(profilerClass, "exitTestMethod", MethodType.methodType(void.class));
    dump =
        lookup.findStatic(profilerClass, "dump", MethodType.methodType(void.class, String.class));
  }

  public void enterTestMethod(final String test) {
    try {
      enterTestMethod.invokeExact(test);
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to invoke profiler at test enter", e);
    }
  }

  public void exitTestMethod() {
    try {
      exitTestMethod.invokeExact();
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to invoke profiler at test exit", e);
    }
  }

  public void dump() {
    String fileName = "conflicts";
    final String givenFileName = System.getProperty("moira.profiler.filename");
    if (givenFileName != null && !givenFileName.isEmpty()) fileName = givenFileName;
    try {
      dump.invokeExact(fileName);
    } catch (final Throwable e) {
      throw new RuntimeException("Failed to invoke profiler at test exit", e);
    }
  }
}
