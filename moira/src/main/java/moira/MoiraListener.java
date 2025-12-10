package moira;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;

public class MoiraListener extends RunListener {
  private static final Pattern pattern = Pattern.compile("\\s+");
  private final ProfilerProxy profilerProxy;
  private final Set<String> filter;

  public MoiraListener(final ProfilerProxy profilerProxy) {
    this.profilerProxy = profilerProxy;
    final String filterFileName = System.getProperty("moira.profiler.filter.filename");
    if (filterFileName != null && !filterFileName.isEmpty())
      filter = initializeFilter(filterFileName);
    else filter = null;
  }

  @Override
  public void testStarted(final Description description) {
    final String testIdentifier =
        String.format("%s[%s]", description.getClassName(), description.toString());

    if (filter == null || filter.contains(testIdentifier))
      profilerProxy.enterTestMethod(testIdentifier);
  }

  @Override
  public void testFinished(final Description description) {
    final String testIdentifier =
        String.format("%s[%s]", description.getClassName(), description.toString());

    if (filter == null || filter.contains(testIdentifier)) profilerProxy.exitTestMethod();
  }

  private Set<String> initializeFilter(final String fileName) {
    try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
      return lines
          .flatMap(line -> pattern.splitAsStream(line))
          .filter(word -> !word.isEmpty())
          .collect(Collectors.toSet());
    } catch (final IOException e) {
      System.err.println("Warning: failed to read tests filter: " + e.getMessage());
      return null;
    }
  }
}
