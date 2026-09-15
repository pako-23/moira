package moira.service;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Agent {
  private static final String AGENT_JAR = "agent.jar";

  private Agent() {}

  public static String path() {
    try {
      final Path codeLocation =
          Paths.get(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (Files.isDirectory(codeLocation)) return developmentAgent(codeLocation).toString();
      if (!Files.isRegularFile(codeLocation)) throw failedToLocateAgent();

      final Path developmentAgent = findDevelopmentAgent(codeLocation.getParent());
      if (developmentAgent != null) return developmentAgent.toString();

      final Path libraries = codeLocation.getParent();
      if (libraries == null || libraries.getParent() == null) throw failedToLocateAgent();
      final Path agent = libraries.getParent().resolve(Paths.get("agent", AGENT_JAR));
      if (!Files.isRegularFile(agent)) throw failedToLocateAgent();
      return agent.toString();
    } catch (final URISyntaxException e) {
      throw failedToLocateAgent(e);
    }
  }

  private static Path developmentAgent(final Path codeLocation) {
    final Path agent = findDevelopmentAgent(codeLocation);
    if (agent != null) return agent;
    throw failedToLocateAgent();
  }

  private static Path findDevelopmentAgent(final Path codeLocation) {
    for (Path parent = codeLocation; parent != null; parent = parent.getParent()) {
      final Path agent = parent.resolve(Paths.get("agent", "build", "libs", AGENT_JAR));
      if (Files.isRegularFile(agent)) return agent;
    }
    return null;
  }

  private static RuntimeException failedToLocateAgent() {
    return new RuntimeException("failed to locate agent, check your installation");
  }

  private static RuntimeException failedToLocateAgent(final Exception cause) {
    return new RuntimeException("failed to locate agent, check your installation", cause);
  }
}
