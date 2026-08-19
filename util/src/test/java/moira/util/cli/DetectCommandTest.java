package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DetectCommandTest extends AbstractMoiraSubcommandTest {
  @BeforeEach
  public void setup() {
    super.setup();
    this.subcommand = "detect";
    this.description = "Detect dependencies between tests";
  }

  @Test
  public void testPackedMode() {
    final int code = cmd.execute("detect", "-m", "packed", "testsuite");

    assertSuccessfulExecution(code);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsTestsuiteParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "<source>", "The file containing the testsuite or the list of test pairs")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsAppClasspathParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern("--app-cp=<classpath>", "The application's classpath")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"-h", "--help"})
  public void testHelpPageContainsModeParameter(final String help) {
    final int code = cmd.execute("detect", help);

    assertSuccessfulExecution(code);
    assertThat(
        stdout.toString(),
        matchesPattern(
            optionDescriptionPattern(
                "-m, --mode=<mode>",
                "Dependency detection algorithm. Valid values are: packed, class-only, intra-class, inter-class, target-pairs, moira (default: packed)")));
  }
}
