package moira.schedules;

import moira.model.TestCase;

public interface ScheduleGenerator {
  public TestCase[] generate();

  public int count();
}
