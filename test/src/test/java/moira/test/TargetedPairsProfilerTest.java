package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TargetedPairsProfilerTest {
  @Test
  public void testMultiplePassingTest() throws IOException, InterruptedException {
    final String fileName = "targeted-pairs-no-dependency";
    final Process process =
        TestUtils.moiraTargetedPairsProfilerCommand(
            fileName, "com.example.SimplePassingTest", "com.example.OtherPassingTest");
    assertThat(process.waitFor(), is(0));
    assertThat(TestUtils.readFileLines(fileName).size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException, InterruptedException {
    final String fileName = "targeted-pairs-static-field-dependency";
    final Process process =
        TestUtils.moiraTargetedPairsProfilerCommand(fileName, "com.example.AppStaticFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(4));
    assertThat(
        lines,
        hasItems(
            "from: com.example.AppStaticFieldTest[testReadFieldX(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testWriteFieldX(com.example.AppStaticFieldTest)]",
            "from: com.example.AppStaticFieldTest[testReadFieldY(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testWriteFieldY(com.example.AppStaticFieldTest)]",
            "from: com.example.AppStaticFieldTest[testWriteFieldX(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testReadFieldX(com.example.AppStaticFieldTest)]",
            "from: com.example.AppStaticFieldTest[testWriteFieldY(com.example.AppStaticFieldTest)], to: com.example.AppStaticFieldTest[testReadFieldY(com.example.AppStaticFieldTest)]"));
  }

  @Test
  public void testObjectFieldDependency() throws IOException, InterruptedException {
    final String fileName = "targeted-pairs-object-field-dependency";
    final Process process =
        TestUtils.moiraTargetedPairsProfilerCommand(fileName, "com.example.AppObjectFieldTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(12));
    assertThat(
        lines,
        hasItems(
            "from: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)]",
            "from: com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)], to: com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]"));
  }

  @Test
  public void testArrayDependency() throws IOException, InterruptedException {
    final String fileName = "targeted-pairs-array-dependency";
    final Process process =
        TestUtils.moiraTargetedPairsProfilerCommand(fileName, "com.example.AppArrayTest");
    assertThat(process.waitFor(), is(0));
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(12));
    assertThat(
        lines,
        hasItems(
            "from: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]",
            "from: com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)], to: com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)]"));
  }
}
