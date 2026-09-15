package moira.factory;

import java.io.File;
import moira.collector.FlakyPairsCollector;
import moira.schedules.ScheduleGenerator;
import moira.service.Service;

public interface MoiraFactory {
  public Service createService();

  public ScheduleGenerator createScheduleGenerator(
      final DetectionMode mode, final Service service, final File source);

  public FlakyPairsCollector createFlakyPairsCollector(final DetectionMode mode, final File source);
}
