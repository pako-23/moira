package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import org.junit.jupiter.api.Test;

public class MoiraListTest extends MoiraTest {

  private static final Map<String, List<TestCase>> tests;

  static {
    tests = new HashMap<>();

    registerJunit4Test(
        com.example.AppArrayTest.class,
        "testWriteFirstIndex",
        "testWriteSecondIndex",
        "testReadFirstIndex",
        "testReadSecondIndex");
    registerJunit4Test(
        com.example.AppObjectFieldTest.class,
        "testReadFieldX",
        "testWriteFieldX",
        "testReadFieldY",
        "testWriteFieldY");
    registerJunit4Test(
        com.example.AppStaticFieldTest.class,
        "testReadFieldX",
        "testWriteFieldX",
        "testReadFieldY",
        "testWriteFieldY");
    registerJunit4Test(com.example.ArrayDependencyTest.class, "something");
    registerJunit4Test(com.example.OtherPassingTest.class, "testPass1", "testPass2");
    registerJunit4Test(com.example.SimplePassingTest.class, "testPass1", "testPass2");
    registerJunit4Test(com.example.SimpleFailingTest.class, "testFail");
    registerJunit4Test(
        com.example.ConcreteClassTest.class,
        "testSomethingAbstract",
        "testOnlyInConcrete",
        "testSomething");
  }

  @Test
  public void testListsAllTests() throws IOException {
    final File testsuite = File.createTempFile("moira", ".txt");
    testsuite.deleteOnExit();

    Files.write(testsuite.toPath(), tests.keySet(), StandardCharsets.UTF_8);
    final int code =
        cmd.execute("list", "--app-cp", System.getProperty("app.classpath"), testsuite.toString());

    assertThat(code, is(0));
    assertThat(stderr.toString(), is(emptyString()));

    assertThat(stdout.toString(), not(is(emptyString())));
    final List<TestCase> detected =
        Stream.of(stdout.toString().trim().split("\\n"))
            .map(id -> new TestCase(id))
            .collect(Collectors.toList());

    assertThat(
        detected,
        containsInAnyOrder(
            tests.values().stream().flatMap(values -> values.stream()).toArray(TestCase[]::new)));
  }

  private static void registerJunit4Test(final Class<?> testClass, final String... descriptions) {
    tests.put(
        testClass.getName(),
        Stream.of(descriptions)
            .map(name -> String.format("%s(%s)", name, testClass.getName()))
            .map(description -> new TestCase(testClass.getName(), description))
            .collect(Collectors.toList()));
  }
}
