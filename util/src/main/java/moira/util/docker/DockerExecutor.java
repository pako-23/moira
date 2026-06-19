package moira.util.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.SELContext;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.util.CompressArchiveUtil;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DockerExecutor {
  private static final SELContext selinuxContext = detectSelinuxContext();

  private final DockerClient client;
  private final List<Bind> binds;
  private final String image;

  public DockerExecutor(final String classpath) {
    this.client = createAndTestDockerClient();
    this.binds = computeVolumeBinds(classpath);
    this.image = createApplicationImage(classpath);
  }

  public DockerExecution execution() {
    return new DockerExecution(this);
  }

  public DockerClient createDockerClient() {
    final DockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    final DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .connectionTimeout(Duration.ofSeconds(5))
            .responseTimeout(Duration.ofSeconds(5))
            .build();

    return DockerClientImpl.getInstance(config, httpClient);
  }

  public List<Bind> getVolumeBinds() {
    return binds;
  }

  public String getImage() {
    return image;
  }

  private DockerClient createAndTestDockerClient() {
    final DockerClient client = createDockerClient();

    try {
      client.pingCmd().exec();
    } catch (final Exception e) {
      throw new RuntimeException("failed to setup docker connection: " + e.getMessage());
    }

    return client;
  }

  private List<Bind> computeVolumeBinds(final String classpath) {
    final List<Bind> binds = new ArrayList<>();

    for (final String path : fullClasspath(classpath)) {
      final File file = new File(path);
      if (isApplicationClassFiles(file)) continue;

      binds.add(
          new Bind(
              file.getAbsolutePath(),
              new Volume(file.getAbsolutePath()),
              AccessMode.ro,
              selinuxContext));
    }

    return binds;
  }

  private boolean isApplicationClassFiles(final File file) {
    final File parent = file.getParentFile();

    return parent != null && parent.getName().equals("target");
  }

  private String[] fullClasspath(final String classpath) {

    return String.join(":", classpath, System.getProperty("java.class.path")).split(":");
  }

  private static SELContext detectSelinuxContext() {
    final File selinuxDir = new File("/sys/fs/selinux");

    if (selinuxDir.isDirectory()) return SELContext.shared;
    return SELContext.none;
  }

  private String createApplicationImage(final String classpath) {
    final File applicationDirectory = findApplicationDirectory(classpath);
    final String imageName = getImageName(applicationDirectory);
    if (imageExists(imageName)) return imageName;

    buildApplicationImage(classpath, applicationDirectory);
    return imageName;
  }

  private File findApplicationDirectory(final String classpath) {
    for (final String path : classpath.split(":")) {
      final File file = new File(path).getAbsoluteFile();
      if (!isApplicationClassFiles(file)) continue;

      final File targetDirectory = file.getParentFile();
      if (targetDirectory == null) continue;

      final File applicationDirectory = targetDirectory.getParentFile();
      if (applicationDirectory == null) continue;

      return applicationDirectory;
    }

    throw new RuntimeException("failed to detect application directory");
  }

  private String getImageName(final File applicationDirectory) {
    return applicationDirectory.getName();
  }

  private void buildApplicationImage(final String classpath, final File applicationDirectory) {
    final String baseImage = getBaseImageName();
    if (!imageExists(baseImage)) pullImage(baseImage);

    final CreateContainerResponse container =
        client
            .createContainerCmd(baseImage)
            .withEntrypoint("java", "-cp", String.join(":", fullClasspath(classpath)))
            .withWorkingDir("/app")
            .exec();

    try {
      try (final InputStream uploadStream =
          Files.newInputStream(createApplicationArchive(applicationDirectory))) {
        client.copyArchiveToContainerCmd(container.getId()).withTarInputStream(uploadStream).exec();
      } catch (final IOException e) {
        throw new RuntimeException("failed to create application image: " + e.getMessage());
      }

      client.commitCmd(container.getId()).withRepository(getImageName(applicationDirectory)).exec();
    } finally {
      client.removeContainerCmd(container.getId()).withForce(true).exec();
    }
  }

  private Path createApplicationArchive(final File applicationDirectory) {
    try {
      final Path archive = Files.createTempFile("", ".tar.gz");

      CompressArchiveUtil.tar(applicationDirectory.toPath(), archive, true, true);

      return archive;
    } catch (final IOException e) {
      throw new RuntimeException("failed to create application archive: " + e.getMessage());
    }
  }

  private void pullImage(final String name) {
    try {
      client.pullImageCmd(name).start().awaitCompletion();
    } catch (final InterruptedException e) {
      throw new RuntimeException("failed to pull docker image " + name + ": " + e.getMessage());
    }
  }

  private boolean imageExists(final String name) {
    return !client.listImagesCmd().withReferenceFilter(name).exec().isEmpty();
  }

  private String getBaseImageName() {
    return "eclipse-temurin:" + getJavaVersion() + "-jre";
  }

  private String getJavaVersion() {
    final String version = System.getProperty("java.version");
    if (version.startsWith("1.")) return version.substring(2, 3);
    else return version.substring(0, version.indexOf("."));
  }
}
