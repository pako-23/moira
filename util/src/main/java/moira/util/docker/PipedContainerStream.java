package moira.util.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;

public abstract class PipedContainerStream implements ContainerStream {
  private final PipedOutputStream pipe;

  public PipedContainerStream() {
    this.pipe = new PipedOutputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    return pipe;
  }

  @Override
  public CompletableFuture<Void> getFuture() {

    return CompletableFuture.<Void>supplyAsync(
        () -> {
          readContainerLogs();
          return null;
        });
  }

  private void readContainerLogs() {
    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(new PipedInputStream(pipe)))) {
      String line;
      while ((line = reader.readLine()) != null) processLine(line);

    } catch (final IOException e) {
      throw new RuntimeException("failed to receive logs from the container: " + e.getMessage());
    }
  }

  protected abstract void processLine(final String line);
}
