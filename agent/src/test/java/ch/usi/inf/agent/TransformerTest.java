package ch.usi.inf.agent;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.instrument.IllegalClassFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransformerTest {

  private Transformer transformer;

  @BeforeEach
  public void setUp() {
    transformer = new Transformer();
  }

  @Test
  public void instrumentFilteredClass() throws IllegalClassFormatException {
    byte[] result = transformer.transform(null, "ch/usi/inf/agent/Agent", null, null, new byte[10]);
    assertNull(result);
  }
}
