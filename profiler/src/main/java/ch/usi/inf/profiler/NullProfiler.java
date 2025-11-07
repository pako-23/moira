package ch.usi.inf.profiler;

public final class NullProfiler {
  private NullProfiler() {}

  public static void suspend() {}

  public static void resume() {}

  public static void writeStaticField(String field) {}

  public static void writeArrayElement(Object array, int index) {}

  public static void writeObjectField(Object object, String field) {}

  public static void readStaticField(String field) {}

  public static void readArrayElement(Object array, int index) {}

  public static void readObjectField(Object object, String field) {}

  public static void enterTestMethod(String test) {}

  public static void exitTestMethod() {}

  public static void dump(final String fileName) {}
}
