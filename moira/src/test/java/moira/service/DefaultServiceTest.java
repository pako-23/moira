package moira.service;

import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import moira.execution.Execution;
import moira.execution.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public abstract class DefaultServiceTest {

  @Mock protected Executor executor;
  protected Service service;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    service = new DefaultService(executor);
  }

  protected MockedExecution[] captureExecutions(final int n) {
    if (n == 0) return new MockedExecution[0];
    final MockedExecution[] executions = new MockedExecution[n];

    for (int i = 0; i < n; ++i) executions[i] = new MockedExecution();

    when(executor.execution())
        .thenReturn(
            executions[0].getExecution(),
            Stream.of(executions)
                .skip(1)
                .map(MockedExecution::getExecution)
                .toArray(Execution[]::new))
        .thenThrow(new IllegalStateException("unexpected execution"));

    return executions;
  }
}
