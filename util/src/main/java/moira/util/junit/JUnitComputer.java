package moira.util.junit;

import org.junit.runner.Computer;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class JUnitComputer extends Computer {
  @Override
  public Runner getSuite(final RunnerBuilder builder, final Class<?>[] classes)
      throws InitializationError {
    return new JUnitSuite(builder, classes);
  }
}
