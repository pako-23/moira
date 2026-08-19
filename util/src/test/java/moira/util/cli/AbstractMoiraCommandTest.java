package moira.util.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;
import moira.util.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import picocli.CommandLine;

public class AbstractMoiraCommandTest {
  @Mock protected Service service;
  protected MoiraUtil moira;
  protected CommandLine cmd;
  protected StringWriter stderr;
  protected StringWriter stdout;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    moira = new MoiraUtil(service);
    cmd = new CommandLine(moira);
    stderr = new StringWriter();
    stdout = new StringWriter();

    cmd.setErr(new PrintWriter(stderr));
    cmd.setOut(new PrintWriter(stdout));
  }

  protected Pattern optionDescriptionPattern(final String option, final String description) {
    return Pattern.compile(
        ".*\\s+"
            + option
            + "\\s+"
            + description.replaceAll("\\s+", "\\\\s+").replace("(", "\\(").replace(")", "\\)")
            + "\\..*",
        Pattern.DOTALL);
  }

  protected void assertSuccessfulExecution(final int code) {
    assertThat(code, is(0));
    assertThat(stderr.toString(), is(emptyString()));
  }

  protected void assertFailedExecution(final int code) {
    assertThat(code, not(is(0)));
    assertThat(stderr.toString(), not(is(emptyString())));
  }
}
