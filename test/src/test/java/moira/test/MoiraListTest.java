package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import moira.util.cli.MoiraUtil;
import moira.util.execution.ForkExecutor;
import moira.util.model.IndexedTestCase;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import moira.util.service.DefaultService;
import moira.util.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

public class MoiraListTest {

  private static Map<String, TestCase[]> tests;
  private CommandLine cmd;
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
    final Service service = new DefaultService(new ForkExecutor());

    stdout = new StringWriter();
    cmd = new CommandLine(new MoiraUtil(service));
    cmd.setOut(new PrintWriter(stdout));

    testsuite = File.createTempFile("list-acceptance-", ".txt");
    testsuite.deleteOnExit();
  }

  @Test
  public void testEmptyTestSuite() throws IOException {
    Files.write(testsuite.toPath(), tests.keySet());

    final int exitCode =
        cmd.execute("list", "--app-cp", System.getProperty("app.classpath"), testsuite.toString());

    assertThat(exitCode, is(0));

    final List<TestCase> listed =
        Arrays.asList(stdout.toString().trim().split("\\n")).stream()
            .map(TestCase::fromId)
            .collect(Collectors.toList());

    final TestCase[] expected =
        tests.values().stream()
            .flatMap(cases -> Arrays.asList(cases).stream())
            .toArray(TestCase[]::new);

    System.out.println(stdout.toString());
    assertThat(listed.size(), is(expected.length));
    assertThat(listed, hasItems(expected));
  }

  @Test
  public void testMultipleTestClasses() {
    final int exitCode =
        cmd.execute("list", "--app-cp", System.getProperty("app.classpath"), testsuite.toString());

    assertThat(exitCode, is(0));
  }

  private static void registerTestCase(final String testClass, final String... descriptions) {
    tests.put(
        testClass,
        Arrays.asList(descriptions).stream()
            .map(description -> String.format("%s[%s(%s)]", testClass, description, testClass))
            .map(TestCase::fromId)
            .toArray(TestCase[]::new));
  }
}
