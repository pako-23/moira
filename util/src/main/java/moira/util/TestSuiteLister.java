package moira.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

public class TestSuiteLister {
  private final List<TestMethod> testsuite;

  public TestSuiteLister(final File suiteFile) throws IOException {
    testsuite = new ArrayList<>();
    final Class<?>[] classes =
        Files.lines(suiteFile.toPath())
            .map(String::trim)
            .map(
                className -> {
                  try {
                    return Class.forName(className);
                  } catch (final ClassNotFoundException e) {
                    throw new IllegalArgumentException(
                        "could not find test class: " + e.getMessage());
                  }
                })
            .toArray(Class<?>[]::new);
    listTestMethods(classes);
  }

  public TestSuiteLister(final Class<?>... classes) {
    testsuite = new ArrayList<>();
    listTestMethods(classes);
  }

  public List<TestMethod> getTestMethods() {
    return testsuite;
  }

  private void listTestMethods(final Class<?>... classes) {
    final Request request =
        Request.classes(classes)
            .filterWith(
                new Filter() {
                  @Override
                  public String describe() {
                    return "list filter";
                  }

                  @Override
                  public boolean shouldRun(final Description description) {
                    if (description.isSuite()) return true;

                    testsuite.add(new TestMethod(TestMethod.descriptionToTestID(description)));

                    return false;
                  }
                });
    final JUnitCore junit = new JUnitCore();

    junit.run(request);
  }
}
