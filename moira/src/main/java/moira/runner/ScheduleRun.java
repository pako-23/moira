package moira.runner;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import moira.model.Outcome;
import moira.model.TestCase;

public interface ScheduleRun {
  public ScheduleRun withOrder(final Comparator<TestCase> comparator);

  public ScheduleRun withFilter(final Predicate<TestCase> predicate);

  public ScheduleRun withTestStartedCallback(final Consumer<TestCase> callback);

  public ScheduleRun withTestFinishedCallback(final Consumer<TestCase> callback);

  public List<Outcome> run();
}
