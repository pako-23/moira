package moira.util.tuscan;

import moira.util.TestCase;

public interface SchedulesGenerator {
  public boolean done();

  public TestCase[] generate();
}
