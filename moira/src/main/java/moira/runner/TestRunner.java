package moira.runner;

public interface TestRunner {
  public ScheduleRun request(final String... testClasses);
}
