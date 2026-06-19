package moira.util.list;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import moira.util.docker.DockerExecutor;
import moira.util.docker.PipedContainerStream;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;

public class TestSuiteBuilder {

  private DockerExecutor executor;
  private File testClassesFile;

  private TestSuiteBuilder() {
    this.executor = null;
  }

  public static TestSuiteBuilder builder() {
    return new TestSuiteBuilder();
  }

  public TestSuiteBuilder withDockerExecutor(final DockerExecutor executor) {
    this.executor = executor;
    return this;
  }

  public TestSuiteBuilder withTestClassesFile(final File file) {
    this.testClassesFile = file;
    return this;
  }

  public TestSuite build() {
    if (executor == null) throw new RuntimeException("missing docker executor");
    else if (testClassesFile == null || !testClassesFile.exists())
      throw new RuntimeException("missing test classes file");

    final List<TestCase> cases = new ArrayList<>();

    try {

      executor
          .execution()
          .withArguments("moira.util.list.TestCasesLister")
          .withStdIn(Files.newInputStream(testClassesFile.toPath()))
          .withStdOut(
              new PipedContainerStream() {

                @Override
                protected void processLine(final String line) {
                  cases.add(new TestCase(line));
                }
              })
          .exec();

      return new TestSuite(cases);
    } catch (final IOException e) {
      throw new RuntimeException("failed to list tests with the testsuite: " + e.getMessage());
    }
  }
}
