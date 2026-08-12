package moira.util.execution;

import java.io.IOException;
import java.util.List;

public interface ProcessFactory {
  public Process create(final List<String> command) throws IOException;
}
