package moira.junit;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

public class SuiteMethodBuilder extends RunnerBuilder {
  @Override
  public Runner runnerForClass(Class<?> each) throws Throwable {
    if (!hasSuiteMethod(each)) return null;

    return new SuiteMethod(each);
  }

  public boolean hasSuiteMethod(Class<?> testClass) {
    try {
      testClass.getMethod("suite");
    } catch (final NoSuchMethodException e) {
      return false;
    }
    return true;
  }
}
