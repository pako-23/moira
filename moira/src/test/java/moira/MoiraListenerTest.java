package moira;

import static org.mockito.Mockito.inOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.Description;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MoiraListenerTest {
  private static final String FILTER_PROPERTY = "moira.profiler.filter.filename";

  @Mock private ProfilerProxy proxy;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void cleanup() {
    System.clearProperty("moira.profiler.filter.filename");
  }

  @ParameterizedTest
  @NullSource
  @EmptySource
  @ValueSource(strings = {"some-not-existing-file"})
  public void testDefaultListener(final String fileName) {
    final Description description =
        Description.createTestDescription(MoiraListenerTest.class, "test");
    final InOrder order = inOrder(proxy);
    final String identifier =
        String.format("%s[%s]", MoiraListenerTest.class.getName(), description.toString());

    if (fileName != null) System.setProperty(FILTER_PROPERTY, fileName);

    final MoiraListener listener = new MoiraListener(proxy);

    listener.testStarted(description);
    listener.testFinished(description);
    order.verify(proxy).enterTestMethod(identifier);
    order.verify(proxy).exitTestMethod();
    order.verifyNoMoreInteractions();
  }

  @Test
  public void testFilterSomeTests() throws IOException {
    final File file = new File("simple-filter-test");
    final Description firstDescription =
        Description.createTestDescription(MoiraListenerTest.class, "testFirst");
    final Description secondDescription =
        Description.createTestDescription(MoiraListenerTest.class, "testSecond");
    final String secondIdentifier =
        String.format("%s[%s]", MoiraListenerTest.class.getName(), secondDescription.toString());
    file.deleteOnExit();
    Files.write(
        file.toPath(), Arrays.asList(secondIdentifier + "   ", " other.TestClass[otherMethod]"));
    System.setProperty(FILTER_PROPERTY, file.toString());

    final InOrder order = inOrder(proxy);
    final MoiraListener listener = new MoiraListener(proxy);

    listener.testStarted(firstDescription);
    listener.testFinished(firstDescription);
    listener.testStarted(secondDescription);
    listener.testFinished(secondDescription);
    order.verify(proxy).enterTestMethod(secondIdentifier);
    order.verify(proxy).exitTestMethod();
    order.verifyNoMoreInteractions();
  }
}
