package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

public class AgentTest {
  @Mock private Instrumentation instrumentationMock;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void cleanup() {
    System.clearProperty("moira.profiler.name");
  }

  @Test
  public void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<Agent> constructor = Agent.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"SomeProfiler"})
  @EmptySource
  @NullSource
  public void testNoRetransformationSupported(final String profiler)
      throws UnmodifiableClassException {
    when(instrumentationMock.isRetransformClassesSupported()).thenReturn(false);
    if (profiler != null) System.setProperty("moira.profiler.name", profiler);

    try (final MockedConstruction<Transformer> transformerMock =
        mockConstruction(
            Transformer.class,
            (mock, context) -> {
              assertThat(context.arguments().size(), is(1));
              final String argument = (String) context.arguments().get(0);
              if (profiler == null || profiler.isEmpty())
                assertThat(argument, is("moira/profiler/NullProfiler"));
              else assertThat(argument, is("moira/profiler/" + profiler));
            })) {
      Agent.premain("", instrumentationMock);
      final List<Transformer> transformers = transformerMock.constructed();
      final InOrder order = inOrder(instrumentationMock);
      assertThat(transformers.size(), is(1));
      order.verify(instrumentationMock).addTransformer(transformers.get(0), true);
      order.verify(instrumentationMock).isRetransformClassesSupported();
      order.verifyNoMoreInteractions();
    }
  }

  @Test
  public void testClassRetransformation() throws UnmodifiableClassException {
    final Class<?>[] classes = new Class<?>[] {String.class, Integer.class};
    when(instrumentationMock.getAllLoadedClasses()).thenReturn(classes);
    when(instrumentationMock.isRetransformClassesSupported()).thenReturn(true);
    when(instrumentationMock.isModifiableClass(classes[0])).thenReturn(true);
    when(instrumentationMock.isModifiableClass(classes[1])).thenReturn(false);

    try (final MockedConstruction<Transformer> transformerMock =
        mockConstruction(Transformer.class)) {
      Agent.premain("", instrumentationMock);
      final List<Transformer> transformers = transformerMock.constructed();
      final InOrder order = inOrder(instrumentationMock);
      assertThat(transformers.size(), is(1));
      order.verify(instrumentationMock).addTransformer(transformers.get(0), true);
      order.verify(instrumentationMock).isRetransformClassesSupported();
      order.verify(instrumentationMock).getAllLoadedClasses();
      order.verify(instrumentationMock).isModifiableClass(classes[0]);
      order.verify(instrumentationMock).retransformClasses(classes[0]);
      order.verify(instrumentationMock).isModifiableClass(classes[1]);
      order.verifyNoMoreInteractions();
    }
  }
}
