package moira.util.factory;

import java.io.File;
import moira.util.FlakyPairsCollector;
import moira.util.runner.ScheduleGenerator;
import moira.util.service.Service;

public interface MoiraFactory {
  public Service createService();

  public ScheduleGenerator createScheduleGenerator(
      final DetectionMode mode, final Service service, final File source);

  public FlakyPairsCollector createFlakyPairsCollector(final DetectionMode mode, final File source);
}
