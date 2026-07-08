package moira.util.runner;

import moira.util.execution.Executor;

// import moira.util.docker.DockerExecutor;

public class ScheduleRunnerBuilder {

  private Executor executor;
  private ScheduleGenerator generator;

  private ScheduleRunnerBuilder() {
    this.executor = null;
    this.generator = null;
  }

  public static ScheduleRunnerBuilder builder() {
    return new ScheduleRunnerBuilder();
  }

  public ScheduleRunnerBuilder withExecutor(final Executor executor) {
    this.executor = executor;
    return this;
  }

  public ScheduleRunnerBuilder withScheduleGenerator(final ScheduleGenerator generator) {
    this.generator = generator;
    return this;
  }

  public ScheduleRunner build() {
    if (executor == null) throw new RuntimeException("no docker executor provided");
    if (generator == null) throw new RuntimeException("no schedules generator provided");

    return new ScheduleRunner(this);
  }

  public Executor getExecutor() {
    return executor;
  }

  public ScheduleGenerator getScheduleGenerator() {
    return generator;
  }
}
