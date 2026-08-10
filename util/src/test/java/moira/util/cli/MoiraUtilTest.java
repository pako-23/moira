package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

public class MoiraUtilTest extends AbstractMoiraCommandTest {

  @Test
  public void testCreation() {
    assertThat(moira.service(), is(service));
  }

  @Test
  public void testNoArgumentsExecution() {
    final int code = cmd.execute();

    assertFailedExecution(code);

    final StringWriter expectedStdout = new StringWriter();
    final CommandLine expectedCmd = new CommandLine(new MoiraUtil(service));
    expectedCmd.setOut(new PrintWriter(expectedStdout));
    expectedCmd.execute("-h");

    assertThat(stderr.toString(), is(expectedStdout.toString()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"--version", "-V"})
  public void testVersion(final String flag) {
    final int code = cmd.execute(flag);

    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), containsString("moira 0.0.1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  public void testHelpFlagIsPresent(final String flag) {
    final int code = cmd.execute(flag);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(optionDescriptionPattern("-h, --help", "Display help and exit")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  public void testHelpContainsDescription(final String flag) {
    final int code = cmd.execute(flag);

    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), containsString("Usage: moira [-hV] [COMMAND]"));
    assertThat(
        stdout.toString(),
        containsString("A tool to detect dependencies between tests of a testsuite."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  public void testHelpDescribesVersionFlag(final String flag) {
    final int code = cmd.execute(flag);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(optionDescriptionPattern("-V, --version", "Display version and exit")));
  }
}
