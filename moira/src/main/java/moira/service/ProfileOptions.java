package moira.service;

import java.io.File;

public class ProfileOptions {
  private Profiler profiler;
  private String filter;
  private File testsuite;

  private ProfileOptions() {
    this.profiler = Profiler.NULL;
    this.testsuite = null;
    this.filter = null;
  }

  public static ProfileOptions builder() {
    return new ProfileOptions();
  }

  public ProfileOptions withProfiler(final Profiler profiler) {
    this.profiler = profiler;
    return this;
  }

  public ProfileOptions withTestSuite(final File testsuite) {
    this.testsuite = testsuite;
    return this;
  }

  public ProfileOptions withFilter(final String filter) {
    this.filter = filter;
    return this;
  }

  public Profiler getProfiler() {
    return this.profiler;
  }

  public File getTestSuite() {
    return this.testsuite;
  }

  public String getFilter() {
    return this.filter;
  }
}
