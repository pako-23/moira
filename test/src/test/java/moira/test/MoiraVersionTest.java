package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MoiraVersionTest extends MoiraTest {
  @ParameterizedTest
  @ValueSource(strings = {"-version", "-V"})
  public void testVersionFlags(final String flag) {
    final int code = cmd.execute(flag);

    assertThat(code, is(0));
    assertThat(stdout.toString(), containsString("moira 0.0.1"));
    assertThat(stderr.toString(), is(emptyString()));
  }
}
