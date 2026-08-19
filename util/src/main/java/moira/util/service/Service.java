package moira.util.service;

import java.io.File;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;

public interface Service {

  public void setAppClassPath(final String classpath);

  public TestSuite discoverTestSuite(final File filename);

  public boolean isIndependentPair(final TestCase first, final TestCase second);
}
