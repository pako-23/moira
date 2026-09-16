package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class NullProfilerTest extends AcceptanceTest {

  @Test
  public void testNullProfiler() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppStaticFieldTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("profile", "--profiler", "null", source.toString());

    assertThat(outputLines().size(), is(0));
  }

  @Test
  public void testDefaultProfilerFlagIsNullProfiler() throws IOException {
    Files.write(
        source.toPath(),
        Arrays.asList(
            com.example.AppStaticFieldTest.class.getName(),
            com.example.AppObjectFieldTest.class.getName(),
            com.example.AppArrayTest.class.getName()));

    execute("profile", source.toString());

    assertThat(outputLines().size(), is(0));
  }
}
