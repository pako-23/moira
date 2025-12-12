package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ObjectProfilerTest {
  @Test
  public void testMultiplePassingTest() throws IOException, InterruptedException {
    final String fileName = "obj-no-dependency";
    final Process process =
        TestUtils.moiraObjectProfilerCommand(
            fileName, "com.example.SimplePassingTest", "com.example.OtherPassingTest");
    assertThat(process.waitFor(), is(0));
    assertThat(TestUtils.readFileLines(fileName).size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException, InterruptedException {
    final String fileName = "obj-static-field-dependency";
    final Process process =
        TestUtils.moiraObjectProfilerCommand(fileName, "com.example.AppStaticFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Arrays.asList(
                "com.example.AppStaticFieldTest[testReadFieldX(com.example.AppStaticFieldTest)] com.example.AppStaticFieldTest[testWriteFieldX(com.example.AppStaticFieldTest)]",
                "com.example.AppStaticFieldTest[testReadFieldY(com.example.AppStaticFieldTest)] com.example.AppStaticFieldTest[testWriteFieldY(com.example.AppStaticFieldTest)]")));
  }

  @Test
  public void testObjectFieldDependency() throws IOException, InterruptedException {
    final String fileName = "obj-object-field-dependency";
    final Process process =
        TestUtils.moiraObjectProfilerCommand(fileName, "com.example.AppObjectFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        is(
            Arrays.asList(
                "com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]",
                "com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
                "com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]",
                "com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]")));
  }

  @Test
  public void testArrayDependency() throws IOException, InterruptedException {
    final String fileName = "obj-array-dependency";
    final Process process =
        TestUtils.moiraObjectProfilerCommand(fileName, "com.example.AppArrayTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        is(
            Arrays.asList(
                "com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
                "com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]",
                "com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
                "com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]")));
  }
}
