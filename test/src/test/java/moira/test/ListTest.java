package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.example.TestAppRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.model.TestCase;
import org.junit.jupiter.api.Test;

public class ListTest extends AcceptanceTest {

  @Test
  public void testMultipleTestClasses() throws IOException {
    final List<String> tests =
        Stream.of(
                com.example.AppArrayTest.class,
                com.example.AppObjectFieldTest.class,
                com.example.AppStaticFieldTest.class,
                com.example.OtherPassingTest.class,
                com.example.SimplePassingTest.class,
                com.example.SimpleFailingTest.class,
                com.example.JUnit4SubclassTest.class,
                com.example.JUnit3SuiteTestAll.class)
            .map(testClass -> testClass.getName())
            .collect(Collectors.toList());

    Files.write(source.toPath(), tests);

    execute("list", source.toString());

    final List<TestCase> listed =
        outputLines().stream().map(TestCase::fromId).collect(Collectors.toList());

    final TestCase[] expected =
        tests.stream()
            .flatMap(
                test ->
                    Arrays.asList(TestAppRegistry.getTestCases(test)).stream()
                        .map(TestCase::fromId))
            .toArray(TestCase[]::new);

    assertThat(listed.size(), is(expected.length));
    assertThat(listed, hasItems(expected));
  }

  @Test
  public void testEmptyTestSuite() {
    execute("list", source.toString());

    assertThat(stdout.toString(), emptyString());
  }
}
