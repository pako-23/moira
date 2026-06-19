package moira.util.docker;

import com.github.dockerjava.api.DockerClient;

public class DockerExecution {

  private final DockerExecutor executor;
  private final DockerClient client;

  public DockerExecution(final DockerExecutor executor) {
    this.executor = executor;
    this.client = executor.createDockerClient();
  }
}
