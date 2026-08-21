package moira.util.service;

import java.io.File;
import java.util.Map;
import java.util.Set;
import moira.util.collector.FlakyPairsCollector;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.schedules.ScheduleGenerator;

public interface Service {

  public void setAppClassPath(final String classpath);

  public void setLogger(final Logger logger);

  public TestSuite discoverTestSuite(final File filename);

  public boolean isIndependentPair(final TestCase first, final TestCase second);

  public void findFlakyPairs(
      final ScheduleGenerator generator, final FlakyPairsCollector collector);

  public Map<TestCase, Set<TestCase>> profile(final Profiler profiler, final File filename);
}
