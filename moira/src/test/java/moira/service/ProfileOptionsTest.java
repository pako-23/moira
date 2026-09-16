package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import org.junit.jupiter.api.Test;

public class ProfileOptionsTest {

  @Test
  public void testDefaults() {
    final ProfileOptions options = ProfileOptions.builder();

    assertThat(options.getProfiler(), is(Profiler.NULL));
    assertThat(options.getTestSuite(), is(nullValue()));
    assertThat(options.getFilter(), is(nullValue()));
  }

  @Test
  public void testSetProfiler() {
    final ProfileOptions options = ProfileOptions.builder();

    assertThat(options.withProfiler(Profiler.ONLINE), is(sameInstance(options)));
    assertThat(options.getProfiler(), is(Profiler.ONLINE));
  }

  @Test
  public void testSetTestSuite() {
    final ProfileOptions options = ProfileOptions.builder();
    final File testsuite = new File("testsuite");

    assertThat(options.withTestSuite(testsuite), is(sameInstance(options)));
    assertThat(options.getTestSuite(), is(sameInstance(testsuite)));
  }

  @Test
  public void testSetFilter() {
    final ProfileOptions options = ProfileOptions.builder();

    assertThat(options.withFilter("com/example/"), is(sameInstance(options)));
    assertThat(options.getFilter(), is("com/example/"));
  }
}
