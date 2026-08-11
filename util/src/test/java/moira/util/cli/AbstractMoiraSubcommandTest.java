package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public abstract class AbstractMoiraSubcommandTest extends AbstractMoiraCommandTest {
  protected String subcommand = null;
  protected String description = null;

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsHelpOption(final String help) {
    final int code = cmd.execute("verify", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(optionDescriptionPattern("-h, --help", "Display help and exit")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsCommandDescription(final String help) {
    final int code = cmd.execute(subcommand, help);

    assertSuccessfulExecution(code);
    assertThat(stdout.toString(), containsString(description));
  }

  @Test
  public void testSubcommandIsListedInMainHelpPage() {
    final int code = cmd.execute("--help");

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(), matchesPattern(optionDescriptionPattern(subcommand, description)));
  }
}
