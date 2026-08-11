package moira.util.service;

import java.io.File;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;

public interface Service {
  public TestSuite discoverTestSuite(final File filename, final String classpath);

  public boolean isIndependentPair(
      final TestCase first, final TestCase second, final String classpath);
}
