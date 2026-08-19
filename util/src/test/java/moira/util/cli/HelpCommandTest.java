package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

public class HelpCommandTest extends AbstractMoiraCommandTest {

  private CommandLine expectedCmd;
  private StringWriter expectedStdout;

  @BeforeEach
  public void setup() {
    super.setup();

    expectedCmd = new CommandLine(new MoiraUtil(factory));
    expectedStdout = new StringWriter();

    expectedCmd.setOut(new PrintWriter(expectedStdout));
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  public void testHelpSubcommandIsListed(final String flag) {
    final int code = cmd.execute(flag);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "help", "Display help information about the specified command")));
  }

  @Test
  public void testInvalidCommand() {
    final int code = cmd.execute("help", "somecommand");

    assertFailedExecution(code);
    assertThat(stderr.toString(), containsString("Unknown subcommand 'somecommand'"));
  }

  @Test
  public void testNoArgumentsGiven() {
    final int code = cmd.execute("help");

    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), not(is(emptyString())));

    expectedCmd.execute("-h");
    assertThat(stdout.toString(), is(expectedStdout.toString()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"list", "verify", "profile", "detect"})
  public void testCommandHelpPage(final String command) {
    final int code = cmd.execute("help", command);

    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), not(is(emptyString())));

    expectedCmd.execute(command, "-h");
    assertThat(stdout.toString(), is(expectedStdout.toString()));
  }
}
