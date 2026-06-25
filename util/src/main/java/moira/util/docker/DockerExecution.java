package moira.util.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DockerExecution {

  private final DockerExecutor executor;
  private final DockerClient client;
  private final List<String> arguments;

  private InputStream stdin;
  private ContainerStream stdout;
  private ContainerStream stderr;

  public DockerExecution(final DockerExecutor executor) {
    this.executor = executor;
    this.client = executor.createDockerClient();
    this.arguments = new ArrayList<>();
    this.stdin = null;
    this.stdout = null;
    this.stderr = null;
  }

  public DockerExecution withArguments(final String... arguments) {
    this.arguments.clear();
    this.arguments.addAll(Stream.of(arguments).collect(Collectors.toList()));
    return this;
  }

  public DockerExecution withStdIn(final InputStream stdin) {
    this.stdin =
        new InputStream() {
          private boolean done = false;
          private InputStream stream = stdin;
          private byte[] trailer = "__END__\n".getBytes();
          private int trailerIndex = 0;

          @Override
          public int read() throws IOException {
            if (!done) {
              final int b = stream.read();
              if (b != -1) return b;
              done = true;
            }

            if (trailerIndex < trailer.length) return trailer[trailerIndex++];

            return -1;
          }
        };
    return this;
  }

  public DockerExecution withStdOut(final ContainerStream stdout) {
    this.stdout = stdout;
    return this;
  }

  public DockerExecution withStdErr(final ContainerStream stderr) {
    this.stderr = stderr;
    return this;
  }

  public void exec() {
    final CreateContainerResponse container = createContainer();
    final AttachContainerCmd command =
        client
            .attachContainerCmd(container.getId())
            .withStdErr(stderr != null)
            .withStdOut(stdout != null)
            .withFollowStream(true);

    if (stdin != null) command.withStdIn(stdin);

    final ResultCallback.Adapter<Frame> callback = getOutputCallback();

    command.exec(callback);

    client.startContainerCmd(container.getId()).exec();

    try {
      callback.awaitCompletion();
      callback.close();
    } catch (final Exception e) {
      throw new RuntimeException("failed to run container: " + e.getMessage());
    } finally {
      client.removeContainerCmd(container.getId()).withForce(true).exec();
    }
  }

  private CreateContainerResponse createContainer() {
    return client
        .createContainerCmd(executor.getImage())
        .withAttachStderr(stderr != null)
        .withAttachStdout(stdout != null)
        .withAttachStdin(stdin != null)
        .withStdinOpen(stdin != null)
        .withTty(false)
        .withCmd(arguments)
        .withHostConfig(
            HostConfig.newHostConfig().withOomScoreAdj(200).withBinds(executor.getVolumeBinds()))
        .exec();
  }

  private ResultCallback.Adapter<Frame> getOutputCallback() {
    return new ResultCallback.Adapter<Frame>() {
      @Override
      public void onNext(final Frame frame) {
        final ContainerStream stream = getContainerStream(frame);
        if (stream != null) stream.append(frame.getPayload());
        super.onNext(frame);
      }

      private ContainerStream getContainerStream(final Frame frame) {
        switch (frame.getStreamType()) {
          case STDERR:
            return stderr;
          case STDOUT:
            return stdout;
          default:
            return null;
        }
      }
    };
  }
}
