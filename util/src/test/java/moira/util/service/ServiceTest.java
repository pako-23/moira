package moira.util.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.execution.Execution;
import moira.util.execution.Executor;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

public class ServiceTest {

  @Mock private Executor executor;
  @Mock private Execution execution;
  @Captor private ArgumentCaptor<InputStream> stdin;
  @Captor private ArgumentCaptor<Consumer<String>> stdout;
  @Captor private ArgumentCaptor<String> arguments;
  private File testsuite;
  private Service service;

  private static final TestCase[] tests =
      new TestCase[] {
        new TestCase("com.example.ExampleTest[somedescription]"),
        new TestCase("com.example.ExampleTest[secondtest]"),
        new TestCase("com.example.ExampleTest2[sometest]"),
        new TestCase("com.example.ExampleTest3[sometest]"),
        new TestCase("com.example.ExampleTest3[testsomething]"),
        new TestCase("com.example.ExampleTest4[testagain]"),
        new TestCase("com.example.ExampleTest4[againsometest]"),
        new TestCase("com.example.ExampleTest5[test]"),
      };

  @BeforeEach
  public void setup() throws IOException {
    MockitoAnnotations.openMocks(this);
    when(executor.execution()).thenReturn(execution);
    when(execution.withStdOut(stdout.capture())).thenReturn(execution);
    when(execution.withStdIn(stdin.capture())).thenReturn(execution);
    when(execution.withArguments(arguments.capture())).thenReturn(execution);

    service = new DefaultService(executor);

    testsuite = File.createTempFile("list-service-", ".txt");
    testsuite.deleteOnExit();
  }

  @ParameterizedTest
  @MethodSource("provideTestSuiteLengths")
  public void testListsTestCasesWithinTestSuite(final int n) {
    doAnswer(writeTestCasesToStdout(n)).when(execution).exec();

    final TestSuite suite = service.discoverTestSuite(testsuite, "");
    assertTestsListed(suite, n);
  }

  @Test
  public void testTestsListingClassInArguments() {
    service.discoverTestSuite(testsuite, "");

    assertThat(arguments.getAllValues(), contains("moira.util.list.TestCasesLister"));
  }

  @ParameterizedTest
  @MethodSource("provideTestSuiteLengths")
  public void testTestSuiteFileBeingPassedAsStdIn(final int n) throws IOException {
    final List<String> lines =
        Arrays.stream(tests)
            .limit(n)
            .map(TestCase::getTestClass)
            .distinct()
            .collect(Collectors.toList());

    Files.write(testsuite.toPath(), lines);
    service.discoverTestSuite(testsuite, "");

    if (lines.isEmpty()) assertThat(getStdInContent(), emptyString());
    else assertThat(getStdInContent(), is(String.join("\n", lines) + "\n"));
  }

  @Test
  public void testNotExistingTestSuiteFile() {
    final RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> service.discoverTestSuite(new File("not-existing"), ""));

    assertThat(exception.getMessage(), containsString("failed to open testsuite file"));
  }

  private Answer<Void> writeTestCasesToStdout(final int n) {
    return invocation -> {
      for (int i = 0; i < n; ++i) stdout.getValue().accept(tests[i].toString());
      return null;
    };
  }

  private void assertTestsListed(final TestSuite suite, final int n) {
    assertThat(suite.numberOfTestCases(), is(n));
    for (int i = 0; i < n; ++i) assertThat(suite.getTestCase(i), is(tests[i]));
  }

  private static Stream<Arguments> provideTestSuiteLengths() {
    return Stream.of(Arguments.of(0), Arguments.of(1), Arguments.of(2), Arguments.of(tests.length));
  }

  private String getStdInContent() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int len;
    while ((len = stdin.getValue().read(chunk)) != -1) buffer.write(chunk, 0, len);

    return new String(buffer.toByteArray());
  }
}
