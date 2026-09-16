package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DetectTest extends AcceptanceTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "tuscan-packed",
        "tuscan-class-only",
        "tuscan-intra-class",
        "tuscan-inter-class",
        "target-pairs",
        "moira"
      })
  public void testEmptyTestSuite(final String mode) {
    execute("detect", "--mode", mode, source.toString());

    assertThat(outputLines().size(), is(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"tuscan-packed", "tuscan-inter-class"})
  public void testTuscanSquare(final String mode) throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("detect", "--mode", mode, source.toString());

    assertThat(
        outputLines(),
        hasItems(
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
                "victim"),
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldY"),
                "victim"),
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteFirstIndex"),
                testId(com.example.AppArrayTest.class, "testReadFirstIndex"),
                "brittle"),
            dependency(
                testId(com.example.AppArrayTest.class, "testWriteSecondIndex"),
                testId(com.example.AppArrayTest.class, "testReadSecondIndex"),
                "brittle")));
  }

  @Test
  public void testTuscanIntraClass() throws IOException {
    Files.write(source.toPath(), Arrays.asList(com.example.AppObjectFieldTest.class.getName()));

    execute("detect", "--mode", "tuscan-intra-class", source.toString());

    assertThat(
        outputLines(),
        hasItems(
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldX"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldX"),
                "victim"),
            dependency(
                testId(com.example.AppObjectFieldTest.class, "testWriteFieldY"),
                testId(com.example.AppObjectFieldTest.class, "testReadFieldY"),
                "victim")));
  }

  @Test
  public void testTuscanClassOnly() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppArrayTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppStaticFieldTest.class.getName()));

    execute("detect", "--mode", "tuscan-class-only", source.toString());

    assertThat(outputLines().size(), is(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"target-pairs", "moira"})
  public void testPairModes(final String mode) throws IOException {
    final String arrayWriter = testId(com.example.AppArrayTest.class, "testWriteFirstIndex");
    final String arrayReader = testId(com.example.AppArrayTest.class, "testReadFirstIndex");
    final String objectWriter = testId(com.example.AppObjectFieldTest.class, "testWriteFieldX");
    final String objectReader = testId(com.example.AppObjectFieldTest.class, "testReadFieldX");

    Files.write(
        source.toPath(),
        Arrays.asList(
            pair(arrayWriter, arrayReader),
            pair(arrayReader, arrayWriter),
            pair(objectWriter, objectReader),
            pair(objectReader, objectWriter)));

    execute("detect", "--mode", mode, source.toString());

    assertThat(
        outputLines(),
        containsInAnyOrder(
            dependency(arrayWriter, arrayReader, "brittle"),
            dependency(objectWriter, objectReader, "victim")));
  }
}
