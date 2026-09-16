package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class NaiveProfilerTest extends AcceptanceTest {

  @Test
  public void testMultiplePassingTests() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.SimplePassingTest.class.getName(),
            com.example.OtherPassingTest.class.getName()));

    execute("profile", "--profiler", "naive", source.toString());

    assertThat(outputLines().size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppStaticFieldTest.class.getName()));

    execute("profile", "--profiler", "naive", source.toString());

    final List<String> lines = outputLines();
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        containsInAnyOrder(
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldX")),
            dependency(
                testId(com.example.AppStaticFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppStaticFieldTest.class, "testReadFieldY"))));
  }

  @Test
  public void testObjectFieldDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppObjectFieldTest.class.getName()));

    execute("profile", "--profiler", "naive", source.toString());

    final List<String> lines = outputLines();
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        containsInAnyOrder(
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldX")),
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldY"))));
  }

  @Test
  public void testArrayDependency() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppArrayTest.class.getName()));

    execute("profile", "--profiler", "naive", source.toString());

    final List<String> lines = outputLines();
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        containsInAnyOrder(
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteFirstIndex"),
                testId(com.example.AppArrayTest.class, "testReadFirstIndex")),
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteSecondIndex"),
                testId(com.example.AppArrayTest.class, "testReadSecondIndex"))));
  }
}
