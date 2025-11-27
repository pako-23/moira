package ch.usi.inf.profiler;

public final class NullProfiler {
  private NullProfiler() {}

  public static void suspend() {}

  public static void resume() {}

  public static void writeStaticField(final String field) {}

  public static void writeArrayElement(final Object array, final int index) {}

  public static void writeObjectField(final Object object, final String field) {}

  public static void readStaticField(final String field) {}

  public static void readArrayElement(final Object array, final int index) {}

  public static void readObjectField(final Object object, final String field) {}

  public static void enterTestMethod(final String test) {}

  public static void exitTestMethod() {}

  public static void dump(final String fileName) {}
}
