package moira.util.runner;

import moira.util.TestCase;

public interface ScheduleGenerator {
  public boolean done();

  public TestCase[] generate();
}
