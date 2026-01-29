package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class OnlineProfilerTest {
  @Test
  public void testMultiplePassingTest() throws IOException, InterruptedException {
    final String fileName = "online-no-dependency";
    final Process process =
        TestUtils.moiraOnlineProfilerCommand(
            fileName, "com.example.SimplePassingTest", "com.example.OtherPassingTest");
    assertThat(process.waitFor(), is(0));
    assertThat(TestUtils.readFileLines(fileName).size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException, InterruptedException {
    final String fileName = "online-static-field-dependency";
    final Process process =
        TestUtils.moiraOnlineProfilerCommand(fileName, "com.example.AppStaticFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Stream.of(
                    "from: com.example.AppStaticFieldTest[testReadFieldX(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testWriteFieldX(com.example.AppStaticFieldTest)]",
                    "from: com.example.AppStaticFieldTest[testReadFieldY(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testWriteFieldY(com.example.AppStaticFieldTest)]")
                .sorted()
                .collect(Collectors.toList())));
  }

  @Test
  public void testObjectFieldDependency() throws IOException, InterruptedException {
    final String fileName = "online-object-field-dependency";
    final Process process =
        TestUtils.moiraOnlineProfilerCommand(fileName, "com.example.AppObjectFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Stream.of(
                    "from: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
                    "from: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]")
                .sorted()
                .collect(Collectors.toList())));
  }

  @Test
  public void testArrayDependency() throws IOException, InterruptedException {
    final String fileName = "online-array-dependency";
    final Process process =
        TestUtils.moiraOnlineProfilerCommand(fileName, "com.example.AppArrayTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Stream.of(
                    "from: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
                    "from: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]")
                .sorted()
                .collect(Collectors.toList())));
  }
}
