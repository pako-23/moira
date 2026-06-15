package moira.util;

import moira.util.runner.ScheduleRunner;

public interface FlakyPairsCollector {
  public void update(final ScheduleRunner.Outcome[] outcome);

  public void print();
}
