package moira.util.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForkExecutor implements Executor {
  private final String classpath;

  public ForkExecutor(final String classpath) {
    this.classpath = computeClassPath(classpath);
  }

  public Execution execution() {
    return new ForkExecution(this);
  }

  private String getClassPath() {
    return classpath;
  }

  private String computeClassPath(final String classpath) {
    return String.join(
        ":",
        classpath,
        Stream.of(System.getProperty("java.class.path").split(":"))
            .map(File::new)
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(":")));
  }

  private class ForkExecution implements Execution {

    private final List<String> command;
    private InputStream stdin;
    private Consumer<String> stdout;

    public ForkExecution(final ForkExecutor executor) {
      this.command = new ArrayList<>();

      command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
      command.add("-classpath");
      command.add(executor.getClassPath());
    }

    @Override
    public void exec() {
      try {
        final Process process = new ProcessBuilder(command).start();

        if (stdin != null) {
          final byte[] buffer = new byte[8192];
          int len;
          while ((len = stdin.read(buffer)) != -1) process.getOutputStream().write(buffer, 0, len);

          process.getOutputStream().close();
          stdin.close();
        }

        if (stdout != null) {
          try (final BufferedReader reader =
              new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) stdout.accept(line);
          }
        }

        process.waitFor();
      } catch (final IOException | InterruptedException e) {
        throw new RuntimeException("failed to run process", e);
      }
    }

    @Override
    public Execution withArguments(final String... args) {
      while (command.size() > 3) command.remove(command.size() - 1);
      for (final String arg : args) command.add(arg);
      return this;
    }

    @Override
    public Execution withStdIn(final InputStream stdin) {
      this.stdin = stdin;
      return this;
    }

    @Override
    public Execution withStdOut(final Consumer<String> stdout) {
      this.stdout = stdout;
      return this;
    }
  }
}
