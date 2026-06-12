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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @Test
  public void testConstructorIsPrivate() throws NoSuchMethodException {
    Constructor<Agent> constructor = Agent.class.getDeclaredConstructor();
    assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
  }

  @Test
  public void testNoRetransformationSupported() throws UnmodifiableClassException {
    when(instrumentationMock.isRetransformClassesSupported()).thenReturn(false);

    try (final MockedConstruction<Transformer> transformerMock =
        mockConstruction(Transformer.class)) {
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
