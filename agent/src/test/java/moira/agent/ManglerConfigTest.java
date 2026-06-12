package moira.agent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ManglerConfigTest {

  @AfterEach
  public void cleanup() {
    System.clearProperty("moira.profiler.name");
    System.clearProperty("moira.agent.filter");
    System.clearProperty("moira.agent.suspend");
  }

  @Test
  public void testDefaultProfiler() {
    final ManglerConfig config = new ManglerConfig();

    assertThat(config.getProfiler(), is("moira/profiler/NullProfiler"));
  }

  @Test
  public void testEmptyProfiler() {
    System.setProperty("moira.profiler.name", "");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.getProfiler(), is("moira/profiler/NullProfiler"));
  }

  @Test
  public void testConfiguredProfiler() {
    System.setProperty("moira.profiler.name", "SomeProfiler");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.getProfiler(), is("moira/profiler/SomeProfiler"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader"
      })
  public void testShouldNotMangleClassNameNullFilter(final String className) {
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"java/util/HashMap", "java/util/Map"})
  public void testShouldMangleClassNameNullFilter(final String className) {
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader"
      })
  public void testShouldNotMangleClassNameEmptyFilter(final String className) {
    System.setProperty("moira.agent.filter", "");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"java/util/HashMap", "java/util/Map"})
  public void testShouldMangleClassNameEmptyFilter(final String className) {
    System.setProperty("moira.agent.filter", "");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader",
        "java/util/HashMap",
        "java/util/Map"
      })
  public void testShouldNotMangleClassNameSingleFilter(final String className) {
    System.setProperty("moira.agent.filter", "java");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader",
        "java/util/HashMap",
        "java/util/Map",
        "com/example/Something"
      })
  public void testShouldNotMangleClassNameMultipleFilter(final String className) {
    System.setProperty("moira.agent.filter", "java/,com/example");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.shouldMangle(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "java/lang/ClassLoader",
        "java/lang/invoke/MethodHandleNatives",
        "java/net/URLClassLoader",
        "java/security/SecureClassLoader",
      })
  public void testShouldSuspendDefault(final String className) {
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.isSuspended(className), is(true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "java/lang/ClassLoader",
        "java/lang/invoke/MethodHandleNatives",
        "java/net/URLClassLoader",
        "java/security/SecureClassLoader",
      })
  public void testShouldSuspendEmptySuspend(final String className) {
    System.setProperty("moira.agent.suspend", "");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.isSuspended(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader",
        "java/util/HashMap",
        "java/util/Map",
        "com/example/Something"
      })
  public void testShouldNotSuspend(final String className) {
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.isSuspended(className), is(false));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira/agent/Agent",
        "moira/agent/Transformer",
        "java/lang/String",
        "org/objectweb/asm/ClassReader",
        "java/util/HashMap",
        "java/util/Map",
      })
  public void testShouldSuspendSuspendListConfig(final String className) {
    System.setProperty("moira.agent.suspend", "moira/,java/,org");
    final ManglerConfig config = new ManglerConfig();
    assertThat(config.isSuspended(className), is(true));
  }
}
