package moira.util.junit;

import java.util.ArrayList;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.internal.builders.JUnit3Builder;
import org.junit.runner.Runner;

public class JUnitRunnerBuilder extends AllDefaultPossibilitiesBuilder {
  protected SuiteMethodBuilder suiteMethodBuilder() {
    return new SuiteMethodBuilder();
  }

  protected JUnit3Builder junit3Builder() {
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
}
