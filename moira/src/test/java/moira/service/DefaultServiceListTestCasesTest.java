package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;
import moira.model.TestCase;
import moira.model.TestSuite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DefaultServiceListTestCasesTest extends DefaultServiceTest {

  private static final TestCase[] tests =
      new TestCase[] {
        TestCase.fromId("com.example.ExampleTest[somedescription]"),
        TestCase.fromId("com.example.ExampleTest[secondtest]"),
        TestCase.fromId("com.example.ExampleTest2[sometest]"),
        TestCase.fromId("com.example.ExampleTest3[sometest]"),
        TestCase.fromId("com.example.ExampleTest3[testsomething]"),
        TestCase.fromId("com.example.ExampleTest4[testagain]"),
        TestCase.fromId("com.example.ExampleTest4[againsometest]"),
        TestCase.fromId("com.example.ExampleTest5[test]"),
      };

  @Test
  public void testTestsListingClassInArguments() throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    service.discoverTestSuite(createTestSuiteFile());

    assertThat(
        executions[0].getArguments(), hasItem(moira.service.TestCasesLister.class.getName()));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 8})
  public void testReturnsTestsWithinTestSuite(final int n) throws IOException {
    final MockedExecution[] executions = captureExecutions(1);

    executions[0].writeStdOutLines(
        Stream.of(tests).limit(n).map(TestCase::toString).toArray(String[]::new));

    final String[] testClasses =
        Stream.of(tests).limit(n).map(TestCase::getTestClass).distinct().toArray(String[]::new);

    final File testsuite = createTestSuiteFile(testClasses);

    final TestSuite suite = service.discoverTestSuite(testsuite);

    assertThat(suite.numberOfTestCases(), is(n));
    for (int i = 0; i < n; ++i) assertThat(suite.getTestCase(i), is(tests[i]));

    assertThat(
        Arrays.asList(executions[0].getStdInContent().trim().split("\\n")), hasItems(testClasses));
  }

  @Test
  public void testNotExistingTestSuiteFile() {
    final RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> service.discoverTestSuite(new File("not-existing")));

    assertThat(exception.getMessage(), containsString("failed to open testsuite file"));
  }

  private static File createTestSuiteFile(final String... testClasses) throws IOException {
    final File testsuite = File.createTempFile("list-service-", ".txt");
    testsuite.deleteOnExit();

    Files.write(testsuite.toPath(), Arrays.asList(testClasses));

    return testsuite;
  }
}
