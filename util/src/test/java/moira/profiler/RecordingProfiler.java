package moira.profiler;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class RecordingProfiler {
  private static final List<String> events = new ArrayList<>();
  private static String currentTest;

  private RecordingProfiler() {}

  public static void enterTestMethod(final String test) {
    currentTest = test;
    events.add("enter: " + test);
  }

  public static void exitTestMethod() {
    events.add("exit: " + currentTest);
    currentTest = null;
  }

  public static void dump(final PrintStream output) {
    for (final String event : events) output.println(event);

    output.flush();
    events.clear();
    currentTest = null;
  }
}
