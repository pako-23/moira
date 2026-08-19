package moira.util.factory;

import java.io.File;
import moira.util.FlakyPairsCollector;
import moira.util.execution.ForkExecutor;
import moira.util.runner.ScheduleGenerator;
import moira.util.service.DefaultService;
import moira.util.service.Service;

public class DefaultFactory implements MoiraFactory {
  @Override
  public Service createService() {
    return new DefaultService(new ForkExecutor());
  }

  @Override
  public ScheduleGenerator createScheduleGenerator(final DetectionMode mode, final File source) {

    return null;
  }

  @Override
  public FlakyPairsCollector createFlakyPairsCollector(
      final DetectionMode mode, final File source) {

    return null;
  }
}
