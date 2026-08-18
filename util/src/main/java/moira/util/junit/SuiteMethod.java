package moira.util.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.junit.internal.runners.JUnit38ClassRunner;

public class SuiteMethod extends JUnit38ClassRunner {
  public SuiteMethod(Class<?> klass) throws Throwable {
    super(testFromSuiteMethod(klass));
  }

  public static Test testFromSuiteMethod(final Class<?> klass) throws Throwable {
    try {
      final Method suiteMethod = klass.getMethod("suite");
      if (!Modifier.isStatic(suiteMethod.getModifiers())) {
        throw new Exception(klass.getName() + ".suite() must be static");
      }

      final Object suite = suiteMethod.invoke(null);

      if (suite instanceof TestSuite) {
        final TestSuite flattenedSuite = new TestSuite(klass.getName());

        flattenTestSuite(flattenedSuite, (TestSuite) suite);

        return flattenedSuite;
      }

      return (Test) suite;

    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static void flattenTestSuite(final TestSuite flattenedSuite, final TestSuite suite) {
    for (int i = 0; i < suite.testCount(); ++i) {
      final Test test = suite.testAt(i);

      if (test instanceof TestSuite) flattenTestSuite(flattenedSuite, (TestSuite) test);
      else flattenedSuite.addTest(test);
    }
  }
}
