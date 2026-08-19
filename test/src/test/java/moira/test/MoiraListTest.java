package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.execution.Execution;
import moira.util.execution.ForkExecutor;
import moira.util.model.IndexedTestCase;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoiraListTest {

  private static Map<String, TestCase[]> tests;
  private Execution execution;
  private File testsuite;
  private StringWriter stdout;

  static {
    tests = new HashMap<>();

    registerTestCase(
        "com.example.AppArrayTest",
        "testWriteFirstIndex",
        "testWriteSecondIndex",
        "testReadFirstIndex",
        "testReadSecondIndex");
    registerTestCase(
        "com.example.AppObjectFieldTest",
        "testReadFieldX",
        "testWriteFieldX",
        "testReadFieldY",
        "testWriteFieldY");
    registerTestCase(
        "com.example.AppStaticFieldTest",
        "testReadFieldX",
        "testWriteFieldX",
        "testReadFieldY",
        "testWriteFieldY");
    registerTestCase("com.example.OtherPassingTest", "testPass1", "testPass2");
    registerTestCase("com.example.SimplePassingTest", "testPass1", "testPass2");
    registerTestCase("com.example.SimpleFailingTest", "testFail");
    registerTestCase(
        "com.example.ConcreteClassTest",
        "testSomething",
        "testSomethingAbstract",
        "testOnlyInConcrete");
    registerTestCase("com.example.JUnit3TestMethodTest", "testSomething");

    tests.put(
        "com.example.JUnit3SuiteTestAll",
        new TestCase[] {
          new SimpleTestCase(
              "com.example.JUnit3SuiteTestAll",
              "testSomething(com.example.JUnit3FirstChildSimpleTest)"),
          new SimpleTestCase(
              "com.example.JUnit3SuiteTestAll",
              "testSomethingElse(com.example.JUnit3FirstChildSimpleTest)"),
          new IndexedTestCase(
              "com.example.JUnit3SuiteTestAll",
              "testSomething(com.example.JUnit3ParametrizedTest)",
              0),
          new IndexedTestCase(
              "com.example.JUnit3SuiteTestAll",
              "testSomething(com.example.JUnit3ParametrizedTest)",
              1),
          new IndexedTestCase(
              "com.example.JUnit3SuiteTestAll",
              "testSomething(com.example.JUnit3ParametrizedTest)",
              2),
        });
  }

  @BeforeEach
  public void setup() throws IOException {
    stdout = new StringWriter();
    execution = new ForkExecutor().execution().withStdOut(line -> stdout.append(line + "\n"));
    testsuite = File.createTempFile("list-acceptance-", ".txt");
    testsuite.deleteOnExit();
  }

  @Test
  public void testMultipleTestClasses() throws IOException {
    Files.write(testsuite.toPath(), tests.keySet());

    execute("list", "--app-cp", System.getProperty("app.classpath"), testsuite.toString());

    final List<TestCase> listed =
        Arrays.asList(stdout.toString().trim().split("\\n")).stream()
            .map(TestCase::fromId)
            .collect(Collectors.toList());

    final TestCase[] expected =
        tests.values().stream()
            .flatMap(cases -> Arrays.asList(cases).stream())
            .toArray(TestCase[]::new);

    assertThat(listed.size(), is(expected.length));
    assertThat(listed, hasItems(expected));
  }

  @Test
  public void testEmptyTestSuite() {
    execute("list", "--app-cp", System.getProperty("app.classpath"), testsuite.toString());

    assertThat(stdout.toString(), emptyString());
  }

  private static void registerTestCase(final String testClass, final String... descriptions) {
    tests.put(
        testClass,
        Arrays.asList(descriptions).stream()
            .map(description -> String.format("%s[%s(%s)]", testClass, description, testClass))
            .map(TestCase::fromId)
            .toArray(TestCase[]::new));
  }

  private void execute(final String... args) {
    final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    final List<String> arguments = runtime.getInputArguments();

    execution
        .withArguments(
            Stream.concat(
                    arguments.stream().filter(name -> name.startsWith("-javaagent")),
                    Stream.concat(Stream.of("moira.util.cli.MoiraUtil"), Stream.of(args)))
                .toArray(String[]::new))
        .exec();
  }
}
