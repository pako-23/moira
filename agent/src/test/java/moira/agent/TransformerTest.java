package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

public class TransformerTest {

  private Transformer transformer;

  @BeforeEach
  public void setup() {
    transformer = new Transformer("moira/profiler/NullProfiler");
  }

  private static byte[] classToBytes(final Class<?> clazz) throws IOException {
    final String resourceName = clazz.getName().replace('.', '/') + ".class";
    ClassLoader classLoader = clazz.getClassLoader();
    if (classLoader == null) classLoader = ClassLoader.getSystemClassLoader();

    try (final InputStream stream = classLoader.getResourceAsStream(resourceName)) {
      try (final ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[4096];
        int read;

        while ((read = stream.read(buffer)) != -1) {
          sink.write(buffer, 0, read);
        }

        return sink.toByteArray();
      }
    }
  }

  private static boolean verifyClass(final byte[] classFileBuffer) {
    final ClassReader reader = new ClassReader(classFileBuffer);
    final StringWriter outputBuffer = new StringWriter();
    final PrintWriter outputWriter = new PrintWriter(outputBuffer);

    CheckClassAdapter.verify(reader, false, outputWriter);

    return outputBuffer.toString().isEmpty();
  }

  @ParameterizedTest
  @ValueSource(classes = {java.util.HashMap.class, java.util.Map.class})
  public void testInstrumentedClass(final Class<?> clazz) throws IOException {
    final byte[] classFileBuffer = classToBytes(clazz);

    assertDoesNotThrow(
        () -> {
          final byte[] instrumented =
              transformer.transform(
                  clazz.getClassLoader(),
                  clazz.getName().replace('.', '/'),
                  clazz,
                  null,
                  classFileBuffer);
          assertThat(instrumented, notNullValue());
          assertThat(verifyClass(instrumented), is(true));
        });
  }

  @ParameterizedTest
  @ValueSource(classes = {java.util.HashMap.class, java.util.Map.class})
  public void testInstrumentedNullClass(final Class<?> clazz) throws IOException {
    final byte[] classFileBuffer = classToBytes(clazz);

    assertDoesNotThrow(
        () -> {
          final byte[] instrumented =
              transformer.transform(clazz.getClassLoader(), null, clazz, null, classFileBuffer);
          assertThat(instrumented, notNullValue());
          assertThat(verifyClass(instrumented), is(true));
        });
  }

  @ParameterizedTest
  @ValueSource(classes = {java.lang.ClassLoader.class, java.net.URLClassLoader.class})
  public void testSuspendedClass(final Class<?> clazz) throws IOException {
    final byte[] classFileBuffer = classToBytes(clazz);

    assertDoesNotThrow(
        () -> {
          final byte[] instrumented =
              transformer.transform(
                  clazz.getClassLoader(),
                  clazz.getName().replace('.', '/'),
                  clazz,
                  null,
                  classFileBuffer);
          assertThat(instrumented, notNullValue());
          assertThat(verifyClass(instrumented), is(true));
        });
  }

  @ParameterizedTest
  @ValueSource(
      classes = {
        moira.agent.Agent.class,
        moira.agent.Transformer.class,
        java.lang.String.class,
        org.objectweb.asm.ClassReader.class
      })
  public void testNotInstrumentedClass(final Class<?> clazz) throws IOException {
    final byte[] classFileBuffer = classToBytes(clazz);

    assertDoesNotThrow(
        () -> {
          final byte[] instrumented =
              transformer.transform(
                  clazz.getClassLoader(),
                  clazz.getName().replace('.', '/'),
                  clazz,
                  null,
                  classFileBuffer);
          assertThat(instrumented, nullValue());
        });
  }

  @Test
  public void testInstrumentationException() {
    final Class<?> clazz = java.util.HashMap.class;

    assertDoesNotThrow(
        () -> {
          final PrintStream originalStderr = System.err;
          final ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
          String message = "";
          System.setErr(new PrintStream(errorBuffer));
          try {
            final byte[] instrumented =
                transformer.transform(
                    clazz.getClassLoader(), clazz.getName().replace('.', '/'), clazz, null, null);
            message = errorBuffer.toString();
            assertThat(instrumented, nullValue());
          } finally {
            System.setErr(originalStderr);
          }

          assertThat(message.isEmpty(), is(false));
        });
  }
}
