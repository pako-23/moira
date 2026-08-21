package moira.util.service;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import moira.util.execution.Execution;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MockedExecution {

  @Mock private Execution execution;
  @Captor private ArgumentCaptor<InputStream> stdin;
  @Captor private ArgumentCaptor<Consumer<String>> stdout;
  @Captor private ArgumentCaptor<String> arguments;

  public MockedExecution() {
    MockitoAnnotations.openMocks(this);

    when(execution.withStdOut(stdout.capture())).thenReturn(execution);
    when(execution.withStdIn(stdin.capture())).thenReturn(execution);
    when(execution.withArguments(arguments.capture())).thenReturn(execution);
  }

  public Execution getExecution() {
    return execution;
  }

  public List<String> getArguments() {
    return arguments.getAllValues();
  }

  public String getStdInContent() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int len;
    while ((len = stdin.getValue().read(chunk)) != -1) buffer.write(chunk, 0, len);

    return new String(buffer.toByteArray());
  }

  public void writeStdOutLines(final String... lines) {
    doAnswer(
            invocation -> {
              for (final String line : lines) stdout.getValue().accept(line);
              return null;
            })
        .when(execution)
        .exec();
  }
}
