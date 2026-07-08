package moira.util.execution;

import java.io.InputStream;
import java.util.function.Consumer;

public interface Execution {
  public Execution withArguments(final String... args);

  public Execution withStdIn(final InputStream stdin);

  public Execution withStdOut(final Consumer<String> stdout);

  public void exec();
}
