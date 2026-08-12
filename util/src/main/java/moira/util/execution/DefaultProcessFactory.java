package moira.util.execution;

import java.io.IOException;
import java.util.List;

public class DefaultProcessFactory implements ProcessFactory {
  @Override
  public Process create(final List<String> command) throws IOException {
    return new ProcessBuilder(command).start();
  }
}
