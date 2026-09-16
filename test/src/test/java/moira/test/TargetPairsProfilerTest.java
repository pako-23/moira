package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TargetPairsProfilerTest extends AcceptanceTest {

  @Test
  public void testMultiplePassingTests() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.SimplePassingTest.class.getName(),
            com.example.OtherPassingTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", source.toString());

    assertThat(outputLines().size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppStaticFieldTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", source.toString());

    final List<String> lines = outputLines();
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        containsInAnyOrder(
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testReadFieldX"),
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldX")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldX")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testReadFieldY"),
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldY")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldY"))));
  }

  @Test
  public void testObjectFieldDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppObjectFieldTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", source.toString());

    final String[] expected =
        directedDependencies(
            com.example.AppObjectFieldTest.class,
            "testReadFieldX",
            "testReadFieldY",
            "testWriteFieldX",
            "testWriteFieldY");
    final List<String> lines = outputLines();
    assertThat(lines.size(), is(expected.length));
    assertThat(lines, containsInAnyOrder(expected));
  }

  @Test
  public void testArrayDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppArrayTest.class.getName()));

    execute("profile", "--profiler", "target-pairs", source.toString());

    final String[] expected =
        directedDependencies(
            com.example.AppArrayTest.class,
            "testWriteFirstIndex",
            "testWriteSecondIndex",
            "testReadFirstIndex",
            "testReadSecondIndex");
    final List<String> lines = outputLines();
    assertThat(lines.size(), is(expected.length));
    assertThat(lines, containsInAnyOrder(expected));
  }

  private static String[] directedDependencies(final Class<?> testClass, final String... methods) {
    final List<String> dependencies = new ArrayList<>();
    for (final String from : methods)
      for (final String to : methods)
        if (!from.equals(to))
          dependencies.add(dependency(testId(testClass, from), testId(testClass, to)));

    return dependencies.toArray(new String[0]);
  }
}
