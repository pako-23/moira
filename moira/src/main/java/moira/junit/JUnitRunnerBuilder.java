package moira.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.internal.builders.AnnotatedBuilder;
import org.junit.internal.builders.IgnoredBuilder;
import org.junit.internal.builders.JUnit3Builder;
import org.junit.internal.builders.JUnit4Builder;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

public class JUnitRunnerBuilder extends RunnerBuilder {

  @Override
  public Runner runnerForClass(final Class<?> testClass) throws Throwable {
    final List<RunnerBuilder> builders =
        Arrays.asList(
            ignoredBuilder(),
            annotatedBuilder(),
            suiteMethodBuilder(),
            junit3Builder(),
            junit4Builder());

    for (final RunnerBuilder each : builders) {
      final Runner runner = each.safeRunnerForClass(testClass);
      if (runner != null) return runner;
    }

    return null;
  }

  protected JUnit4Builder junit4Builder() {
    return new JUnit4Builder();
  }

  private SuiteMethodBuilder suiteMethodBuilder() {
    return new SuiteMethodBuilder();
  }

  private JUnit3Builder junit3Builder() {
    return new JUnit3Builder() {
      @Override
      public Runner runnerForClass(final Class<?> testClass) throws Throwable {
        if (!junit.framework.TestCase.class.isAssignableFrom(testClass)) return null;

        final TestSuite suite = new TestSuite(testClass.asSubclass(TestCase.class));
        final ArrayList<Test> tests = new ArrayList<>();

        for (int i = 0; i < suite.testCount(); ++i) tests.add(suite.testAt(i));

        return new JUnit3Runner(testClass, tests);
      }
    };
  }

  private AnnotatedBuilder annotatedBuilder() {
    return new AnnotatedBuilder(this);
  }

  private IgnoredBuilder ignoredBuilder() {
    return new IgnoredBuilder();
  }
}
