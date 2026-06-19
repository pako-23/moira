package moira.util.runner;

import moira.util.model.TestCase;

public interface ScheduleGenerator {
  public boolean done();

  public TestCase[] generate();
}
