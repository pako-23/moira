package moira.util.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TestCaseTest {
  private static final String[] testClasses =
      new String[] {"moira.util.model.TestCaseTest", "com.example.ExampleTest"};
  private static final String[] descriptions =
      new String[] {"sometestdescription", "otherdescription", "desc[ript]ion", "desc[ription"};

  @ParameterizedTest
  @MethodSource("provideSimpleTestDescriptions")
  public void testSimpleTestCaseCreation(final String testClass, final String description) {
    final SimpleTestCase test = new SimpleTestCase(testClass, description);

    assertThat(test.getTestClass(), is(testClass));
    assertThat(test.toString(), is(String.format("%s[%s]", testClass, description)));
  }

  @ParameterizedTest
  @MethodSource("provideIndexedTestDescriptions")
  public void testIndexedTestCaseCreation(
      final String testClass, final String description, final int index) {
    final IndexedTestCase test = new IndexedTestCase(testClass, description, index);

    assertThat(test.getTestClass(), is(testClass));
    assertThat(test.getIndex(), is(index));
    assertThat(test.toString(), is(String.format("%s[%s]#%d", testClass, description, index)));
  }

  @Test
  public void testSimpleTestCaseEquals() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final SimpleTestCase second = new SimpleTestCase(testClasses[0], descriptions[0]);

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void testSimpleTestCaseEqualsDifferentClass() {
    final Object first = new SimpleTestCase(testClasses[0], descriptions[0]);
    assertThat(first.equals("hello"), is(false));
  }

  @Test
  public void testSimpleTestCaseEqualsDifferentTestClass() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final SimpleTestCase second = new SimpleTestCase(testClasses[1], descriptions[0]);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testSimpleTestCaseEqualsDifferentDescription() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final SimpleTestCase second = new SimpleTestCase(testClasses[0], descriptions[1]);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testSimpleTestCaseEqualsDifferent() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final SimpleTestCase second = new SimpleTestCase(testClasses[1], descriptions[1]);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testIndexedTestCaseEquals() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[0], descriptions[0], 0);

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferentClass() {
    final Object first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    assertThat(first.equals("hello"), is(false));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferentTestClass() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[1], descriptions[0], 0);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferentDescription() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[0], descriptions[1], 0);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferentIndex() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[0], descriptions[0], 1);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferentType() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final IndexedTestCase second = new IndexedTestCase(testClasses[0], descriptions[0], 1);

    assertThat(first.equals(second), is(false));
    assertThat(second.equals(first), is(false));
  }

  @Test
  public void testIndexedTestCaseEqualsDifferent() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[1], descriptions[1], 1);

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void testSimpleTestCaseHashCode() {
    final SimpleTestCase first = new SimpleTestCase(testClasses[0], descriptions[0]);
    final SimpleTestCase second = new SimpleTestCase(testClasses[0], descriptions[0]);

    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void testIndexedTestCaseHashCode() {
    final IndexedTestCase first = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase second = new IndexedTestCase(testClasses[0], descriptions[0], 0);
    final IndexedTestCase third = new IndexedTestCase(testClasses[0], descriptions[0], 1);

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(first.hashCode(), not(is(third.hashCode())));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "moira.util.TestCaseTestsometestdescription]",
        "moira.util.TestCaseTestsometestdescription",
        "moira.util.TestCaseTest[sometestdescription",
        "[somedescription]",
        "com.example.ExampleTest[]",
        "com.example.ExampleTest[somedesc]#",
        "com.example.ExampleTest[somedesc]#ciaone",
        "com.example.ExampleTest[somedesc]##1",
        "com.example.ExampleTest[somedesc]a132",
      })
  public void testTestCaseInvalidFormat(final String identifier) {
    final IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> TestCase.fromId(identifier));

    assertThat(
        exception.getMessage(),
        is(
            "tests should have the form <class-name>[<test-description>] or <class-name>[<test-description>]#<index>"));
  }

  @ParameterizedTest
  @MethodSource("provideTestCases")
  public void testTestCaseFromId(final TestCase test) {
    final String id = test.toString();

    assertThat(TestCase.fromId(id), is(test));
  }

  private static Stream<Arguments> provideSimpleTestDescriptions() {
    return Arrays.asList(testClasses).stream()
        .flatMap(
            testClass ->
                Arrays.asList(descriptions).stream()
                    .map(description -> Arguments.of(testClass, description)));
  }

  private static Stream<Arguments> provideIndexedTestDescriptions() {
    return Arrays.asList(testClasses).stream()
        .flatMap(
            testClass ->
                Arrays.asList(descriptions).stream()
                    .flatMap(
                        description ->
                            Stream.of(0, 1, 4)
                                .map(index -> Arguments.of(testClass, description, index))));
  }

  private static Stream<Arguments> provideTestCases() {
    final Stream<Arguments> simpleTestCases =
        Arrays.asList(testClasses).stream()
            .flatMap(
                testClass ->
                    Arrays.asList(descriptions).stream()
                        .map(
                            description ->
                                Arguments.of(new SimpleTestCase(testClass, description))));

    final Stream<Arguments> indexedTestCases =
        Arrays.asList(testClasses).stream()
            .flatMap(
                testClass ->
                    Arrays.asList(descriptions).stream()
                        .flatMap(
                            description ->
                                Stream.of(0, 1, 4)
                                    .map(
                                        index ->
                                            Arguments.of(
                                                new IndexedTestCase(
                                                    testClass, description, index)))));

    return Stream.concat(simpleTestCases, indexedTestCases);
  }
}
