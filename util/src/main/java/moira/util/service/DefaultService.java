package moira.util.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import moira.util.execution.Executor;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;

public class DefaultService implements Service {

  private final Executor executor;

  public DefaultService(final Executor executor) {
    this.executor = executor;
  }

  @Override
  public TestSuite discoverTestSuite(final File filename, final String classpath) {
    final List<TestCase> tests = new ArrayList<>();
    final InputStream input;

    try {
      input = Files.newInputStream(filename.toPath());
    } catch (final IOException e) {
      throw new RuntimeException("failed to open testsuite file", e);
    }

    executor
        .execution()
        .withStdIn(input)
        .withArguments("moira.util.list.TestCasesLister")
        .withStdOut(line -> tests.add(new TestCase(line)))
        .exec();

    return new TestSuite(tests);
  }

  @Override
  public boolean isIndependentPair(
      final TestCase first, final TestCase second, final String classpath) {
    return false;
  }
}
