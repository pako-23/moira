package ch.usi.inf.moira.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.Description;

public class TestMethodTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "ch.usi.inf.moira.util.TestMethodTest[sometestdescription]",
        "ch.usi.inf.moira.util.TestMethodTest[somete[stdescrip]tion]",
        "ch.usi.inf.moira.util.TestMethod[somete[stdescrip]tion]"
      })
  public void testTestMethodConstructor(final String identifier) throws ClassNotFoundException {
    final TestMethod method = new TestMethod(identifier);
    final Class<?> testClass = Class.forName(identifier.substring(0, identifier.indexOf('[')));

    assertThat(method.toString(), is(identifier));
    assertThat(method.getTestClass(), is(testClass));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "ch.usi.inf.moira.util.TestMethodTestsometestdescription]",
        "ch.usi.inf.moira.util.TestMethodTest[sometestdescription",
        "ch.usi.inf.moira.util.TestMethodTestsometestdescription",
        "[something]"
      })
  public void testTestMethodInvalidFormat(final String identifier) {
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new TestMethod(identifier));

    assertThat(
        thrown.getMessage(), is("tests should have the form <class-name>[<test-description>]"));
  }

  @Test
  public void testTestMethodMissingClass() {
    final String identifier = "aaaaaaaa[description]";
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new TestMethod(identifier));

    assertThat(thrown.getMessage(), is("failed to find testclass for test: " + identifier));
  }

  @Test
  public void testTestMethodMissingDescription() {
    final String identifier = "ch.usi.inf.moira.util.TestMethodTest[]";
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new TestMethod(identifier));

    assertThat(thrown.getMessage(), is("missing description from test: " + identifier));
  }

  private static Stream<Arguments> testTestIdentifierFromDescriptionParams() {
    final Class<?>[] classes = {TestMethod.class, String.class, TestMethodTest.class};

    final String[] names = {"first", "otherTest", "something"};

    return Stream.of(classes)
        .flatMap(clazz -> Stream.of(names).map(name -> Arguments.of(clazz, name)));
  }

  @ParameterizedTest
  @MethodSource("testTestIdentifierFromDescriptionParams")
  public void testTestIdentifierFromDescription(final Class<?> clazz, final String name) {
    final Description description = Description.createTestDescription(clazz, name);
    final String expected =
        String.format("%s[%s]", description.getClassName(), description.toString());
    assertThat(TestMethod.descriptionToTestID(description), is(expected));
  }
}
