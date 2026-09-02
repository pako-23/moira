package moira.util.junit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import junit.framework.Test;
import junit.framework.TestSuite;

public class SuiteMethod extends JUnit3Runner {
  public SuiteMethod(final Class<?> testClass) throws Throwable {
    super(testClass, testFromSuiteMethod(testClass));
  }

  private static ArrayList<Test> testFromSuiteMethod(final Class<?> testClass) throws Throwable {
    try {
      final Method suiteMethod = testClass.getMethod("suite");
      if (!Modifier.isStatic(suiteMethod.getModifiers())) {
        throw new Exception(testClass.getName() + ".suite() must be static");
      }

      final Object suite = suiteMethod.invoke(null);
      final ArrayList<Test> tests = new ArrayList<>();

      if (suite instanceof TestSuite) flattenTestSuite(tests, (TestSuite) suite);
      else tests.add((Test) suite);

      return tests;

    } catch (final InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static void flattenTestSuite(final ArrayList<Test> tests, final TestSuite suite) {
    for (int i = 0; i < suite.testCount(); ++i) {
      final Test test = suite.testAt(i);

      if (test instanceof TestSuite) flattenTestSuite(tests, (TestSuite) test);
      else tests.add(test);
    }
  }
}
