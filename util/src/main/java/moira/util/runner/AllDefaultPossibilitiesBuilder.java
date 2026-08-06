package moira.util.runner;

import java.util.Arrays;
import java.util.List;
import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.internal.builders.IgnoredBuilder;
import org.junit.internal.builders.JUnit3Builder;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

public class AllDefaultPossibilitiesBuilder extends RunnerBuilder {
  @Override
  public Runner runnerForClass(Class<?> testClass) throws Throwable {
    List<RunnerBuilder> builders =
        Arrays.asList(
            ignoredBuilder(),
            annotatedBuilder(),
            suiteMethodBuilder(),
            junit3Builder(),
            junit4Builder());

    for (RunnerBuilder each : builders) {
      Runner runner = each.safeRunnerForClass(testClass);
      if (runner != null) return runner;
    }

    return null;
  }

  protected JUnit4Builder junit4Builder() {
    return new JUnit4Builder();
  }

  protected JUnit3Builder junit3Builder() {
    return new JUnit3Builder();
  }

  protected AnnotatedBuilder annotatedBuilder() {
    return new AnnotatedBuilder(this);
  }

  protected IgnoredBuilder ignoredBuilder() {
    return new IgnoredBuilder();
  }

  protected SuiteMethodBuilder suiteMethodBuilder() {
    return new SuiteMethodBuilder();
  }
}
