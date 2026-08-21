package moira.util.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DefaultServiceSetAppClasspathTest extends DefaultServiceTest {

  @ParameterizedTest
  @ValueSource(strings = {"/app", "/app/classes:/app/test-classes"})
  public void testClasspathIsSetOnExecutor(final String classpath) {
    service.setAppClassPath(classpath);
    verify(executor, times(1)).setClassPath(classpath);
  }
}
