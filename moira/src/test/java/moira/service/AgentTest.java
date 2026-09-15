package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

public class AgentTest {
  private static final String ERROR = "failed to locate agent, check your installation";

  @TempDir Path temporaryDirectory;

  @Test
  public void testPrivateConstructor() throws Exception {
    final Constructor<Agent> constructor = Agent.class.getDeclaredConstructor();

    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));

    constructor.setAccessible(true);
    assertThat(constructor.newInstance(), instanceOf(Agent.class));
  }

  @Test
  public void testDevelopmentClassesDirectoryFindsAgentInAncestor() throws Exception {
    final Path project = temporaryDirectory.resolve("project");
    final Path classes = project.resolve(Paths.get("util", "build", "classes", "java", "main"));
    final Path agent = project.resolve(Paths.get("agent", "build", "libs", "agent.jar"));
    Files.createDirectories(classes);
    Files.createDirectories(agent.getParent());
    Files.createFile(agent);

    assertThat(agentPath(classes.toUri().toURL()), is(agent.toString()));
  }

  @Test
  public void testDevelopmentClassesDirectoryWithoutAgentFails() throws Exception {
    final Path classes = temporaryDirectory.resolve(Paths.get("project", "classes"));
    Files.createDirectories(classes);

    assertFailureWithoutCause(classes.toUri().toURL());
  }

  @Test
  public void testNonRegularCodeLocationFails() throws Exception {
    assertFailureWithoutCause(temporaryDirectory.resolve("missing.jar").toUri().toURL());
  }

  @Test
  public void testPackagedJarPrefersDevelopmentAgent() throws Exception {
    final Path project = temporaryDirectory.resolve("project");
    final Path utility = project.resolve(Paths.get("util", "build", "libs", "util.jar"));
    final Path agent = project.resolve(Paths.get("agent", "build", "libs", "agent.jar"));
    Files.createDirectories(utility.getParent());
    Files.createFile(utility);
    Files.createDirectories(agent.getParent());
    Files.createFile(agent);

    assertThat(agentPath(utility.toUri().toURL()), is(agent.toString()));
  }

  @Test
  public void testPackagedDistributionFindsAgent() throws Exception {
    final Path distribution = temporaryDirectory.resolve("distribution");
    final Path utility = distribution.resolve(Paths.get("lib", "util.jar"));
    final Path agent = distribution.resolve(Paths.get("agent", "agent.jar"));
    Files.createDirectories(utility.getParent());
    Files.createFile(utility);
    Files.createDirectories(agent.getParent());
    Files.createFile(agent);

    assertThat(agentPath(utility.toUri().toURL()), is(agent.toString()));
  }

  @Test
  public void testPackagedDistributionWithoutAgentFails() throws Exception {
    final Path utility = temporaryDirectory.resolve(Paths.get("distribution", "lib", "util.jar"));
    Files.createDirectories(utility.getParent());
    Files.createFile(utility);

    assertFailureWithoutCause(utility.toUri().toURL());
  }

  @Test
  public void testCodeLocationWithoutParentFails() throws Exception {
    final URI codeSource = Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    final Path codeLocation = mock(Path.class);

    try (final MockedStatic<Paths> paths = mockStatic(Paths.class, CALLS_REAL_METHODS);
        final MockedStatic<Files> files = mockStatic(Files.class)) {
      paths.when(() -> Paths.get(codeSource)).thenReturn(codeLocation);
      files.when(() -> Files.isRegularFile(codeLocation)).thenReturn(true);
      when(codeLocation.getParent()).thenReturn(null);

      assertFailureWithoutCause(Agent::path);
    }
  }

  @Test
  public void testCodeLocationWithRootParentFails() throws Exception {
    final URI codeSource = Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    final Path codeLocation = mock(Path.class);
    final Path libraries = mock(Path.class);
    final Path candidate = mock(Path.class);
    final Path relativeAgent = Paths.get("agent", "build", "libs", "agent.jar");

    when(codeLocation.getParent()).thenReturn(libraries);
    when(libraries.resolve(relativeAgent)).thenReturn(candidate);
    when(libraries.getParent()).thenReturn(null);

    try (final MockedStatic<Paths> paths = mockStatic(Paths.class, CALLS_REAL_METHODS);
        final MockedStatic<Files> files = mockStatic(Files.class)) {
      paths.when(() -> Paths.get(codeSource)).thenReturn(codeLocation);
      files.when(() -> Files.isRegularFile(codeLocation)).thenReturn(true);

      assertFailureWithoutCause(Agent::path);
    }
  }

  @Test
  public void testMalformedCodeSourceFailsWithCause() throws Exception {
    final RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> agentPath(new URL("file:/invalid path/util.jar")));

    assertThat(exception.getMessage(), is(ERROR));
    assertThat(exception.getCause(), instanceOf(URISyntaxException.class));
  }

  private static void assertFailureWithoutCause(final URL codeSource) {
    assertFailureWithoutCause(() -> agentPath(codeSource));
  }

  private static void assertFailureWithoutCause(final ThrowingOperation operation) {
    final RuntimeException exception = assertThrows(RuntimeException.class, operation::run);

    assertThat(exception.getMessage(), is(ERROR));
    assertThat(exception.getCause(), nullValue());
  }

  private static String agentPath(final URL codeSource) throws Exception {
    final Class<?> agent = loadAgent(codeSource);
    try {
      return (String) agent.getMethod("path").invoke(null);
    } catch (final InvocationTargetException e) {
      throw (RuntimeException) e.getCause();
    }
  }

  private static Class<?> loadAgent(final URL location) throws Exception {
    final byte[] bytecode = agentBytecode();
    final ClassLoader loader =
        new ClassLoader(null) {
          @Override
          protected Class<?> findClass(final String name) throws ClassNotFoundException {
            if (!Agent.class.getName().equals(name)) throw new ClassNotFoundException(name);
            final CodeSource source = new CodeSource(location, (Certificate[]) null);
            final ProtectionDomain protectionDomain = new ProtectionDomain(source, null);
            return defineClass(name, bytecode, 0, bytecode.length, protectionDomain);
          }
        };
    return loader.loadClass(Agent.class.getName());
  }

  private static byte[] agentBytecode() throws IOException {
    try (final InputStream input = Agent.class.getResourceAsStream("Agent.class");
        final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      final byte[] buffer = new byte[4096];
      for (int read = input.read(buffer); read >= 0; read = input.read(buffer)) {
        output.write(buffer, 0, read);
      }
      return output.toByteArray();
    }
  }

  private interface ThrowingOperation {
    void run() throws Exception;
  }
}
