package moira.test;

import java.io.PrintWriter;
import java.io.StringWriter;
import moira.util.cli.MoiraUtil;
import org.junit.jupiter.api.BeforeEach;
import picocli.CommandLine;

public class MoiraTest {
  protected CommandLine cmd;
  protected StringWriter stdout;
  protected StringWriter stderr;

  @BeforeEach
  public void setup() {
    cmd = new CommandLine(new MoiraUtil());
    stdout = new StringWriter();
    stderr = new StringWriter();
    cmd.setOut(new PrintWriter(stdout));
    cmd.setErr(new PrintWriter(stderr));
  }
}
