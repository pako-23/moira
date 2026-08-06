package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;
import moira.util.cli.MoiraUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

public class MoiraHelpTest extends MoiraTest {
  private CommandLine expectedCmd;
  private StringWriter expectedStdout;
  private StringWriter expectedStderr;

  @BeforeEach
  public void setup() {
    super.setup();

    expectedCmd = new CommandLine(new MoiraUtil());
    expectedStdout = new StringWriter();
    expectedStderr = new StringWriter();
    expectedCmd.setOut(new PrintWriter(expectedStdout));
    expectedCmd.setErr(new PrintWriter(expectedStderr));
  }

  @ParameterizedTest
  @MethodSource("provideHelpArgs")
  public void testHelpForValidCommandEqualsSubcommandHelp(final String command, final String flag) {
    final int expectedCode = expectedCmd.execute("help", command);
    final int code = cmd.execute(command, flag);

    assertThat(expectedCode, is(0));
    assertThat(code, is(expectedCode));

    assertThat(stderr.toString(), is(emptyString()));
    assertThat(stderr.toString(), is(expectedStderr.toString()));

    assertThat(stdout.toString(), not(is(emptyString())));
    assertThat(stdout.toString(), is(expectedStdout.toString()));
  }

  @Test
  public void testHelpForInvalidCommandPrintsUnknownCommand() {
    final int code = cmd.execute("help", "somecommand");

    assertThat(code, not(is(0)));
    assertThat(stderr.toString(), containsString("Unknown subcommand 'somecommand'"));
  }

  @Test
  public void testHelpCommandWithoutArgumentsFails() {
    final int code = cmd.execute("help");

    assertThat(code, is(0));
    assertThat(stderr.toString(), is(emptyString()));
    assertThat(stdout.toString(), not(is(emptyString())));
  }

  @Test
  public void testNoArgumentsGiven() {
    final int code = cmd.execute();
    expectedCmd.execute("help");

    assertThat(code, not(is(0)));
    assertThat(stdout.toString(), is(expectedStdout.toString()));
  }

  private static Stream<Arguments> provideHelpArgs() {
    final String[] flags = new String[] {"-h", "--help"};

    return Stream.of("list", "verify", "tuscan", "help")
        .flatMap(command -> Stream.of(flags).map(flag -> Arguments.of(command, flag)));
  }
}
