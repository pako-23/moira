package moira.util.list;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.util.model.IndexedTestCase;
import moira.util.model.SimpleTestCase;
import moira.util.model.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.Description;

public class TestCasesListerFilterTest {
  private static final String[] classNames =
      new String[] {
        "com.example.ExampleTest",
        "com.example.ExampleInnerTest",
        "com.app.AppTest",
        "com.example.UiTest",
        "com.example.SomeClassTest",
        "com.example.SomeOtherClassTest",
      };
  private static final String[] descriptions =
      new String[] {
        "description",
        "someotherdescription",
        "testSomething",
        "testSomething2",
        "testSomethingAgain",
        "testSomethingIsBlue",
        "testSomethingIsRed"
      };

  private TestCasesListerFilter filter;

  @BeforeEach
  private void setup() {
    filter = new TestCasesListerFilter();
  }

  @Test
  public void testDescription() {
    assertThat(filter.describe(), is("list filter"));
  }

  @ParameterizedTest
  @MethodSource("provideSimpleDescription")
  public void testShouldRunSimpleDescription(final String className, final String[] descriptions) {
    final Description description = generateSimpleDescription(className, descriptions);

    assertThat(filter.shouldRun(description), is(false));
    assertDiscoveredTestCases(filter.getTestCases(), getChildrenDescriptions(description));
  }

  @ParameterizedTest
  @MethodSource("provideSingleNestedDescritions")
  public void testSingleNestedDescription(final Description[] descriptions) {
    final Description description = mock(Description.class);
    final ArrayList<Description> children = new ArrayList<>();
    children.addAll(Arrays.asList(descriptions));

    when(description.getChildren()).thenReturn(children);

    assertThat(filter.shouldRun(description), is(false));
    assertNestedDiscoveredTestCases(filter.getTestCases(), descriptions);
  }

  @ParameterizedTest
  @MethodSource("provideSingleNestedDescritions")
  public void testMultipleShouldRunInvocations(final Description[] descriptions) {
    for (final Description description : descriptions)
      assertThat(filter.shouldRun(description), is(false));
    assertNestedDiscoveredTestCases(filter.getTestCases(), descriptions);
  }

  @ParameterizedTest
  @MethodSource("provideSingleNestedDescritions")
  public void testMultipleDescriptionNesting(final Description[] descriptions) {
    final ArrayList<Description> children = new ArrayList<>();

    for (final Description description : descriptions) {
      final Description intermediate = mock(Description.class);
      final ArrayList<Description> intermediateChildren = new ArrayList<>();
      intermediateChildren.add(description);
      when(intermediate.getChildren()).thenReturn(intermediateChildren);
      children.add(intermediate);
    }

    final Description description = mock(Description.class);
    when(description.getChildren()).thenReturn(children);

    assertThat(filter.shouldRun(description), is(false));
    assertNestedDiscoveredTestCases(filter.getTestCases(), descriptions);
  }

  @Test
  public void testChildrenWithDupilcateDescriptions() {
    final Description description =
        generateSimpleDescription(
            classNames[0],
            descriptions[1],
            descriptions[0],
            descriptions[0],
            descriptions[2],
            descriptions[0]);

    final List<String> expected = new ArrayList<>();
    expected.add(new SimpleTestCase(classNames[0], descriptions[1]).toString());
    expected.add(new IndexedTestCase(classNames[0], descriptions[0], 0).toString());
    expected.add(new IndexedTestCase(classNames[0], descriptions[0], 1).toString());
    expected.add(new SimpleTestCase(classNames[0], descriptions[2]).toString());
    expected.add(new IndexedTestCase(classNames[0], descriptions[0], 2).toString());

    assertThat(filter.shouldRun(description), is(false));
    assertDiscoveredTestCases(filter.getTestCases(), expected);
  }

  private void assertNestedDiscoveredTestCases(
      final List<TestCase> discovered, final Description... descriptions) {

    final int expectedSize =
        Arrays.asList(descriptions).stream()
            .map(description -> description.getChildren().size())
            .reduce(0, (subtotal, length) -> subtotal + length);

    assertThat(discovered.size(), is(expectedSize));

    int begin = 0;
    for (final Description description : descriptions) {
      final List<String> expected = getChildrenDescriptions(description);

      assertDiscoveredTestCases(discovered.subList(begin, begin + expected.size()), expected);
      begin += expected.size();
    }
  }

  private static Stream<Arguments> provideSingleNestedDescritions() {
    final Description descriptions[] =
        new Description[] {
          generateSimpleDescription(
              "com.example.ExampleTest",
              "someotherdescription",
              "testSomething",
              "testSomething2",
              "testSomethingAgain"),
          generateSimpleDescription(
              "com.example.UiTest", "testSomethingIsBlue", "testSomethingIsRed"),
          generateSimpleDescription("com.example.ExmapleInnerTest", "testSimple"),
          generateSimpleDescription("com.example.SomeClassTest", "testSimple", "testAgain"),
          generateSimpleDescription("com.example.SomeOtherClassTest", "testCreation")
        };

    return Stream.of(1, 2, descriptions.length)
        .map(
            length ->
                Arguments.of(
                    (Object)
                        Arrays.asList(descriptions).stream()
                            .limit(length)
                            .toArray(Description[]::new)));
  }

  private static Stream<Arguments> provideSimpleDescription() {
    return Arrays.asList(classNames).stream()
        .limit(3)
        .flatMap(
            className ->
                Stream.of(1, 2, descriptions.length)
                    .map(
                        length ->
                            Arguments.of(
                                className,
                                Arrays.asList(descriptions).stream()
                                    .limit(length)
                                    .toArray(String[]::new))));
  }

  private static Description generateSimpleDescription(
      final String className, final String... descriptions) {
    final Description description = mock(Description.class);
    final ArrayList<Description> children = new ArrayList<>();

    for (int i = 0; i < descriptions.length; ++i) {
      final Description child = mock(Description.class);

      when(child.getClassName()).thenReturn(className);
      when(child.toString()).thenReturn(descriptions[i]);
      children.add(child);
    }

    when(description.getChildren()).thenReturn(children);

    return description;
  }

  private void assertDiscoveredTestCases(
      final List<TestCase> discovered, final List<String> children) {
    assertThat(discovered.size(), is(children.size()));
    for (int i = 0; i < discovered.size(); ++i) {
      final String child = children.get(i);
      assertThat(discovered.get(i).toString(), is(child));
    }
  }

  private static List<String> getChildrenDescriptions(final Description description) {
    return description.getChildren().stream()
        .map(child -> new SimpleTestCase(child.getClassName(), child.toString()).toString())
        .collect(Collectors.toList());
  }
}
