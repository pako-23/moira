package moira.cli;

import java.io.PrintWriter;
import moira.service.Logger;

public class CommandLineLogger implements Logger {

  private final PrintWriter output;

  public CommandLineLogger(final PrintWriter stream) {
    this.output = stream;
  }

  @Override
  public void log(final String line) {
    output.println(line);
  }
}
