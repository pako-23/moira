package moira.util.runner;

import moira.util.docker.DockerExecutor;

public class ScheduleRunnerBuilder {

  public static final int DEFAULT_CONCURRENCY_LEVEL =
      Runtime.getRuntime().availableProcessors() / 2;

  private DockerExecutor executor;
  private ScheduleGenerator generator;
  private int concurrencyLevel;

  private ScheduleRunnerBuilder() {
    this.executor = null;
    this.generator = null;
    this.concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;
  }

  public static ScheduleRunnerBuilder builder() {
    return new ScheduleRunnerBuilder();
  }

  public ScheduleRunnerBuilder withDockerExecutor(final DockerExecutor executor) {
    this.executor = executor;
    return this;
  }

  public ScheduleRunnerBuilder withScheduleGenerator(final ScheduleGenerator generator) {
    this.generator = generator;
    return this;
  }

  public ScheduleRunnerBuilder withConcurrencyLevel(final int concurrencyLevel) {
    if (concurrencyLevel <= 0)
      throw new IllegalArgumentException("concurrency level must be at least 1");
    this.concurrencyLevel = concurrencyLevel;
    return this;
  }

  public ScheduleRunner build() {
    if (executor == null) throw new RuntimeException("no docker executor provided");
    if (generator == null) throw new RuntimeException("no schedules generator provided");

    return new ScheduleRunner(this);
  }

  public DockerExecutor getDockerExecutor() {
    return executor;
  }

  public ScheduleGenerator getScheduleGenerator() {
    return generator;
  }

  public int getConcurrencyLevel() {
    return concurrencyLevel;
  }
}
