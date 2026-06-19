package moira.util.docker;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public interface ContainerStream {
  public OutputStream getOutputStream();

  public CompletableFuture<Void> getFuture();
}
