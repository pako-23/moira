package moira.util.list;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import moira.util.execution.Executor;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;

public class TestSuiteBuilder {

  private Executor executor;
  private File testClassesFile;

  private TestSuiteBuilder() {
    this.executor = null;
  }

  public static TestSuiteBuilder builder() {
    return new TestSuiteBuilder();
  }

  public TestSuiteBuilder withExecutor(final Executor executor) {
    this.executor = executor;
    return this;
  }

  public TestSuiteBuilder withTestClassesFile(final File file) {
    this.testClassesFile = file;
    return this;
  }

  public TestSuite build() {
    if (executor == null) throw new RuntimeException("no executor provided");
    else if (testClassesFile == null || !testClassesFile.exists())
      throw new RuntimeException("missing test classes file");

    final List<TestCase> cases = new ArrayList<>();

    try {
      executor
          .execution()
          .withArguments("moira.util.list.TestCasesLister")
          .withStdIn(Files.newInputStream(testClassesFile.toPath()))
          .withStdOut(
              line -> {
                cases.add(TestCase.fromId(line));
              })
          .exec();

      return new TestSuite(cases);
    } catch (final IOException e) {
      throw new RuntimeException("failed to list tests with the testsuite: " + e.getMessage());
    }
  }
}
