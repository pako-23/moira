package ch.usi.inf.runner;

import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.internal.builders.IgnoredBuilder;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.RunnerBuilder;

public class DelegateRunnerBuilder extends RunnerBuilder {
  @Override
  public Runner runnerForClass(Class<?> testClass) throws Throwable {
    final RunnerBuilder[] builders =
        new RunnerBuilder[] {
          new IgnoredBuilder(), new AnnotatedBuilder(this), new JUnit4Builder(),
        };

    for (RunnerBuilder each : builders) {
      Runner runner = each.safeRunnerForClass(testClass);
      if (runner != null) return runner;
    }

    return null;
  }

  public DelegateRunner delegateRunnerForClass(Class<?> testClass) {
    try {
      Runner runner = runnerForClass(testClass);

      if (runner instanceof BlockJUnit4ClassRunner) return new JUnit4Wrapper(testClass);

      System.err.println("WARNING: not supported runner: " + runner);

      return null;
    } catch (Throwable t) {
      System.err.println("WARNING: feiled to create runner: " + t);
      return null;
    }
  }
}
