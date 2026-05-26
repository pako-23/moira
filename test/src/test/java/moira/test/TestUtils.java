package moira.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {
  private static Process moiraCommand(
      final String profiler, final String dumpFileName, final String... args) throws IOException {
    final List<String> command = new ArrayList<>();

    final RuntimeMXBean parentArgs = ManagementFactory.getRuntimeMXBean();
    command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

    command.addAll(
        parentArgs.getInputArguments().stream()
            .filter(arg -> arg.startsWith("-javaagent:"))
            .collect(Collectors.toList()));
    command.add("-javaagent:" + System.getProperty("moira.agent.path"));
    command.add("-Xbootclasspath/a:" + System.getProperty("moira.agent.path"));
    command.add("-classpath");
    command.add(System.getProperty("java.class.path"));
    if (profiler != null) command.add("-Dmoira.profiler.name=" + profiler);
    if (dumpFileName != null) {
      final File file = new File(dumpFileName);
      file.deleteOnExit();
      command.add("-Dmoira.profiler.filename=" + dumpFileName);
    }
    command.add("moira.Moira");

    final String classListFile = Files.createTempFile("moira-classes-", ".txt").toString();
    Files.write(Paths.get(classListFile), String.join("\n", args).getBytes());
    command.add(classListFile);

    return new ProcessBuilder(command).start();
  }

  public static Process moiraObjectProfilerCommand(final String dumpFileName, final String... args)
      throws IOException {
    return moiraCommand("ObjectProfiler", dumpFileName, args);
  }

  public static Process moiraOnlineProfilerCommand(final String dumpFileName, final String... args)
      throws IOException {
    return moiraCommand("OnlineProfiler", dumpFileName, args);
  }

  public static Process moiraNaiveProfilerCommand(final String dumpFileName, final String... args)
      throws IOException {
    return moiraCommand("NaiveProfiler", dumpFileName, args);
  }

  public static Process moiraTargetedPairsProfilerCommand(
      final String dumpFileName, final String... args) throws IOException {
    return moiraCommand("TargetedPairsProfiler", dumpFileName, args);
  }

  public static Process moiraDefaultsCommand(final String... args) throws IOException {
    return moiraCommand(null, null, args);
  }

  public static String readOutputStream(final InputStream stream) throws IOException {
    final StringBuffer buffer = new StringBuffer();
    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line).append("\n");
      }
    }

    return buffer.toString();
  }

  public static List<String> readFileLines(final String fileName) throws IOException {
    return Files.readAllLines(Paths.get(fileName)).stream().collect(Collectors.toList());
  }

  public static Process moiraUtilCommand(final String... args) throws IOException {
    final List<String> command = new ArrayList<>();
    final RuntimeMXBean parentArgs = ManagementFactory.getRuntimeMXBean();

    command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
    command.addAll(
        parentArgs.getInputArguments().stream()
            .filter(arg -> arg.startsWith("-javaagent:"))
            .collect(Collectors.toList()));
    command.add("-classpath");
    command.add(System.getProperty("java.class.path"));
    command.add("moira.util.cli.MoiraUtil");
    command.addAll(Stream.of(args).collect(Collectors.toList()));

    return new ProcessBuilder(command).start();
  }
}
