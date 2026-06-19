package moira.util.model;

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

public class TestCaseTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "moira.util.model.TestCaseTest[sometestdescription]",
        "moira.util.model.TestCaseTest[somete[stdescrip]tion]",
        "moira.util.model.TestCase[somete[stdescrip]tion]"
      })
  public void testTestCaseConstructor(final String identifier) {
    final TestCase method = new TestCase(identifier);
    final String testClass = identifier.substring(0, identifier.indexOf('['));

    assertThat(method.toString(), is(identifier));
    assertThat(method.getTestClass(), is(testClass));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "moira.util.TestCaseTestsometestdescription]",
        "moira.util.TestCaseTest[sometestdescription",
        "moira.util.TestCaseTestsometestdescription",
        "[something]"
      })
  public void testTestCaseInvalidFormat(final String identifier) {
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new TestCase(identifier));

    assertThat(
        thrown.getMessage(), is("tests should have the form <class-name>[<test-description>]"));
  }

  @Test
  public void testTestCaseMissingDescription() {
    final String identifier = "moira.util.TestCaseTest[]";
    final IllegalArgumentException thrown =
        assertThrows(IllegalArgumentException.class, () -> new TestCase(identifier));

    assertThat(thrown.getMessage(), is("missing description from test: " + identifier));
  }

  private static Stream<Arguments> testTestIdentifierFromDescriptionParams() {
    return Stream.of(TestCase.class, String.class, TestCaseTest.class)
        .flatMap(
            clazz ->
                Stream.of("first", "otherTest", "something")
                    .map(name -> Arguments.of(clazz, name)));
  }

  @ParameterizedTest
  @MethodSource("testTestIdentifierFromDescriptionParams")
  public void testTestIdentifierFromDescription(final Class<?> clazz, final String name) {
    final Description description = Description.createTestDescription(clazz, name);
    final String expected =
        String.format("%s[%s]", description.getClassName(), description.toString());
    assertThat(
        TestCase.identifier(description.getClassName(), description.toString()), is(expected));
  }
}
