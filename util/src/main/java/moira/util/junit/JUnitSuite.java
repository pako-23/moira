package moira.util.junit;

import java.util.List;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class JUnitSuite extends Suite {

  public JUnitSuite(final RunnerBuilder builder, final Class<?>[] classes)
      throws InitializationError {
    super(null, findTestClassesRunners(builder, classes));
  }

  @Override
  protected String getName() {
    return "classes";
  }

  private static List<Runner> findTestClassesRunners(
      final RunnerBuilder builder, final Class<?>[] classes) throws InitializationError {
    final List<Runner> runners = builder.runners(null, classes);

    for (final Runner runner : runners)
      if (runner instanceof ErrorReportingRunner)
        throw new InitializationError("invalid test class: " + runner.getDescription().toString());

    return runners;
  }
}
