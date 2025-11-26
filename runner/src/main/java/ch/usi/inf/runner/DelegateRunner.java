package ch.usi.inf.runner;

import java.lang.reflect.Method;
import org.junit.runner.notification.RunNotifier;

public interface DelegateRunner {
  public void run(Method testCase, RunNotifier notifier);
}
