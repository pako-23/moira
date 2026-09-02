package moira.util.runner;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import moira.util.model.Outcome;
import moira.util.model.TestCase;

public interface ScheduleRun {
  public ScheduleRun withOrder(final Comparator<TestCase> comparator);

  public ScheduleRun withFilter(final Predicate<TestCase> predicate);

  public List<Outcome> run();
}
