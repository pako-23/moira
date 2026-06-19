package moira.util.runner;

import moira.util.model.TestCase;

public interface ScheduleGenerator {
  @Deprecated
  public boolean done();

  public TestCase[] generate();

  public int count();
}
