package ch.usi.inf.runner;

import java.lang.reflect.Method;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

public class JUnit4Wrapper extends BlockJUnit4ClassRunner implements DelegateRunner {
  public JUnit4Wrapper(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  public void run(Method testCase, RunNotifier notifier) {
    final FrameworkMethod method = new FrameworkMethod(testCase);

    runChild(method, notifier);
  }
}
