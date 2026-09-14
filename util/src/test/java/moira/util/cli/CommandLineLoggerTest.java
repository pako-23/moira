package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommandLineLoggerTest {
  private StringWriter output;
  private CommandLineLogger logger;

  @BeforeEach
  public void setup() {
    output = new StringWriter();
    logger = new CommandLineLogger(new PrintWriter(output));
  }

  @Test
  public void testLogWritesLine() {
    logger.log("progress 0/1");

    assertThat(output.toString(), is("progress 0/1" + System.lineSeparator()));
  }

  @Test
  public void testLogWritesMultipleLines() {
    logger.log("progress 0/2");
    logger.log("progress 1/2");

    assertThat(
        output.toString(),
        is("progress 0/2" + System.lineSeparator() + "progress 1/2" + System.lineSeparator()));
  }
}
