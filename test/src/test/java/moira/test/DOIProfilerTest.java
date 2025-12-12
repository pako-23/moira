package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DOIProfilerTest {
  @Test
  public void testMultiplePassingTest() throws IOException, InterruptedException {
    final String fileName = "doi-no-dependency";
    final Process process =
        TestUtils.moiraDOIProfilerCommand(
            fileName, "com.example.SimplePassingTest", "com.example.OtherPassingTest");
    process.waitFor();
    assertThat(TestUtils.readFileLines(fileName).size(), is(0));
  }

  @Test
  public void testStaticFieldDependency() throws IOException, InterruptedException {
    final String fileName = "doi-static-field-dependency";
    final Process process =
        TestUtils.moiraDOIProfilerCommand(fileName, "com.example.AppStaticFieldTest");
    process.waitFor();
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
    final String fileName = "doi-object-field-dependency";
    final Process process =
        TestUtils.moiraDOIProfilerCommand(fileName, "com.example.AppObjectFieldTest");
    process.waitFor();
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Arrays.asList(
                "com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
                "com.example.AppObjectFieldTest[testReadFieldY(com.example.AppObjectFieldTest)] com.example.AppObjectFieldTest[testWriteFieldY(com.example.AppObjectFieldTest)]")));
  }

  @Test
  public void testArrayDependency() throws IOException, InterruptedException {
    final String fileName = "doi-array-dependency";
    final Process process = TestUtils.moiraDOIProfilerCommand(fileName, "com.example.AppArrayTest");
    process.waitFor();
    final List<String> lines = TestUtils.readFileLines(fileName);
    assertThat(lines.size(), is(2));
    assertThat(
        lines,
        is(
            Arrays.asList(
                "com.example.AppArrayTest[testReadFirstIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteFirstIndex(com.example.AppArrayTest)]",
                "com.example.AppArrayTest[testReadSecondIndex(com.example.AppArrayTest)] com.example.AppArrayTest[testWriteSecondIndex(com.example.AppArrayTest)]")));
  }
}
