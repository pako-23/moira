package moira.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.junit.JUnitRunner;
import moira.model.TestCase;
import moira.runner.TestRunner;

public final class AgentRunner {

  private static final Pattern pattern = Pattern.compile("from: |, to: ");
  private static final String DEFAULT_PROFILER = "moira.profiler.NullProfiler";

  private AgentRunner() {}

  public static void main(final String... args)
      throws IOException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
    final PrintStream stdout = System.out;

    System.setOut(System.err);
    run(System.in, stdout);
    System.exit(0);
  }

  public static void run(final InputStream input, final PrintStream output)
      throws IOException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException {
    final String[] classes = readTestClasses(input);
    final TestRunner runner = new JUnitRunner();
    final ProfilerProxy profiler = new ProfilerProxy(detectProfiler());
    final Set<TestCase> filter = detectFilter();

    runner
        .request(classes)
        .withTestStartedCallback(
            test -> {
              if (filter == null || filter.contains(test))
                profiler.enterTestMethod(test.toString());
            })
        .withTestFinishedCallback(
            test -> {
              if (filter == null || filter.contains(test)) profiler.exitTestMethod();
            })
        .run();

    profiler.dump(output);
  }

  private static String[] readTestClasses(final InputStream input) throws IOException {
    final List<String> classes = new ArrayList<>();

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;

      while ((line = reader.readLine()) != null) classes.add(line);
    }

    return classes.stream().toArray(String[]::new);
  }

  private static String detectProfiler() {
    final String profilerName = System.getProperty("moira.profiler.name");

    if (profilerName == null || profilerName.isEmpty()) return DEFAULT_PROFILER;

    return "moira.profiler." + profilerName;
  }

  private static Set<TestCase> detectFilter() {
    final String filterFileName = System.getProperty("moira.profiler.filter.filename");
    if (filterFileName == null || filterFileName.isEmpty()) return null;

    return initializeFilter(filterFileName);
  }

  private static Set<TestCase> initializeFilter(final String fileName) {
    try (final Stream<String> lines = Files.lines(Paths.get(fileName))) {
      return lines
          .flatMap(line -> pattern.splitAsStream(line))
          .filter(word -> !word.isEmpty())
          .map(TestCase::fromId)
          .collect(Collectors.toSet());
    } catch (final IOException e) {
      System.err.println("Warning: failed to read tests filter: " + e.getMessage());
      return null;
    }
  }
}
