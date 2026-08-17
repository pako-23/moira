package moira.util.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForkExecutor implements Executor {
  private String classpath;
  private final ProcessFactory processFactory;

  public ForkExecutor(final ProcessFactory factory) {
    this.classpath = computeClassPath("");
    this.processFactory = factory;
  }

  public ForkExecutor() {
    this(new DefaultProcessFactory());
  }

  @Override
  public Execution execution() {
    return new ForkExecution(this);
  }

  @Override
  public void setClassPath(final String classpath) {
    this.classpath = computeClassPath(classpath);
  }

  private String getClassPath() {
    return classpath;
  }

  private ProcessFactory getProcessFactory() {
    return processFactory;
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
    private Consumer<String> stderr;
    private final ForkExecutor executor;

    public ForkExecution(final ForkExecutor executor) {
      this.command = new ArrayList<>();

      command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
      command.add("-classpath");
      command.add(executor.getClassPath());
      this.stdout = line -> {};
      this.stderr = line -> {};
      this.executor = executor;
    }

    @Override
    public void exec() {
      try {
        final Process process = executor.getProcessFactory().create(command);

        final CompletableFuture<Void> stdinFuture =
            CompletableFuture.supplyAsync(
                () -> {
                  if (stdin == null) return null;

                  try {
                    final byte[] buffer = new byte[8192];
                    int len;
                    while ((len = stdin.read(buffer)) != -1)
                      process.getOutputStream().write(buffer, 0, len);

                    process.getOutputStream().close();
                    stdin.close();
                    return null;
                  } catch (final IOException e) {
                    throw new RuntimeException("failed to send input to the forked process", e);
                  }
                });

        final CompletableFuture<Void> stdoutFuture =
            CompletableFuture.supplyAsync(
                () -> {
                  try (final BufferedReader reader =
                      new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) stdout.accept(line);

                    return null;
                  } catch (final IOException e) {
                    throw new RuntimeException("failed to read from stdout", e);
                  }
                });

        final CompletableFuture<Void> stderrFuture =
            CompletableFuture.supplyAsync(
                () -> {
                  try (final BufferedReader reader =
                      new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) stderr.accept(line);

                    return null;
                  } catch (final IOException e) {
                    throw new RuntimeException("failed to read from stderr", e);
                  }
                });

        stdinFuture.get();
        stdoutFuture.get();
        stderrFuture.get();

        final int code = process.waitFor();
        if (code != 0) throw new RuntimeException("fork execution failed with code " + code);

      } catch (final ExecutionException exec) {
        try {
          throw exec.getCause();
        } catch (final RuntimeException e) {
          throw e;
        } catch (final Throwable e) {
          throw new RuntimeException(e);
        }
      } catch (final IOException | InterruptedException e) {
        throw new RuntimeException("process execution failed", e);
      }
    }

    @Override
    public Execution withArguments(final String... args) {
      while (command.size() > 3) command.remove(command.size() - 1);
      for (final String arg : args) command.add(arg);
      return this;
    }

    @Override
    public Execution withStdErr(final Consumer<String> stderr) {
      this.stderr = stderr;
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
