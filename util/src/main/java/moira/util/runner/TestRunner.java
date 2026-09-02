package moira.util.runner;

public interface TestRunner {
  public ScheduleRun request(final String... testClasses);
}
