package moira.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import moira.util.model.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

public class PairVerifierTest {
  private static final TestCase firstTest =
      new TestCase(
          PairVerifierTest.class.getName() + "[first(" + PairVerifierTest.class.getName() + ")]");
  private static final TestCase secondTest =
      new TestCase(
          PairVerifierTest.class.getName() + "[second(" + PairVerifierTest.class.getName() + ")]");
  private static final TestCase otherTest =
      new TestCase(
          PairVerifier.class.getName() + "[sometest(" + PairVerifier.class.getName() + ")]");

  @Test
  public void testSingleClassTestSchedules() throws ClassNotFoundException {
    final ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
    @SuppressWarnings("unchecked")
    final ArgumentCaptor<Comparator<Description>> comparatorCaptor =
        ArgumentCaptor.forClass(Comparator.class);

    try (final MockedStatic<Request> requestMock = mockStatic(Request.class)) {
      final Request returnedRequest = mock(Request.class);
      when(returnedRequest.filterWith(filterCaptor.capture())).thenReturn(returnedRequest);
      when(returnedRequest.sortWith(comparatorCaptor.capture())).thenReturn(returnedRequest);
      requestMock
          .when(() -> Request.aClass(Class.forName(firstTest.getTestClass())))
          .thenReturn(returnedRequest);

      new PairVerifier(firstTest, secondTest);
      final List<Filter> filters = filterCaptor.getAllValues();
      final List<Comparator<Description>> comparators = comparatorCaptor.getAllValues();

      final Description firstDescription =
          Description.createTestDescription(firstTest.getTestClass(), "first");
      final Description secondDescription =
          Description.createTestDescription(firstTest.getTestClass(), "second");

      assertThat(filters.size(), is(1));
      final Filter filter = filters.get(0);
      assertThat(filter.describe(), is("pair filter"));
      assertThat(filter.shouldRun(firstDescription), is(true));
      assertThat(filter.shouldRun(secondDescription), is(true));
      assertThat(
          filter.shouldRun(Description.createTestDescription(getClass(), "third")), is(false));
      assertThat(
          filter.shouldRun(Description.createTestDescription(PairVerifier.class, "first")),
          is(false));

      assertThat(comparators.size(), is(1));
      assertThat(comparators.get(0).compare(firstDescription, secondDescription), greaterThan(0));
      assertThat(comparators.get(0).compare(secondDescription, firstDescription), lessThan(0));
    }
  }

  @Test
  public void testTwoClassesTestSchedules() throws ClassNotFoundException {
    final ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);

    try (final MockedStatic<Request> requestMock = mockStatic(Request.class)) {
      final Request returnedRequest = mock(Request.class);
      when(returnedRequest.filterWith(filterCaptor.capture())).thenReturn(returnedRequest);
      requestMock
          .when(
              () ->
                  Request.classes(
                      Class.forName(otherTest.getTestClass()),
                      Class.forName(firstTest.getTestClass())))
          .thenReturn(returnedRequest);
      requestMock
          .when(
              () ->
                  Request.classes(
                      Class.forName(firstTest.getTestClass()),
                      Class.forName(otherTest.getTestClass())))
          .thenReturn(returnedRequest);

      new PairVerifier(firstTest, otherTest);
      final List<Filter> filters = filterCaptor.getAllValues();

      final Description firstDescription =
          Description.createTestDescription(firstTest.getTestClass(), "first");
      final Description secondDescription =
          Description.createTestDescription(otherTest.getTestClass(), "sometest");

      assertThat(filters.size(), is(1));
      final Filter filter = filters.get(0);
      assertThat(filter.describe(), is("pair filter"));

      final Description firstSuite = Description.createSuiteDescription(firstTest.getTestClass());
      final Description secondSuite = Description.createSuiteDescription(otherTest.getTestClass());

      firstSuite.addChild(firstDescription);
      secondSuite.addChild(secondDescription);

      assertThat(filter.shouldRun(firstSuite), is(true));
      assertThat(filter.shouldRun(secondSuite), is(true));
      assertThat(filter.shouldRun(firstDescription), is(true));
      assertThat(filter.shouldRun(secondDescription), is(true));
      assertThat(
          filter.shouldRun(Description.createTestDescription(getClass(), "third")), is(false));
      assertThat(
          filter.shouldRun(Description.createTestDescription(PairVerifier.class, "first")),
          is(false));
    }
  }

  private static Stream<Arguments> testVerifyParams() {
    return Stream.of(Arguments.of(firstTest, secondTest), Arguments.of(firstTest, otherTest));
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifyPass(final TestCase first, final TestCase second)
      throws ClassNotFoundException {
    final Result successResult = mock(Result.class);
    when(successResult.wasSuccessful()).thenReturn(true);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              when(mock.run(any(Request.class))).thenReturn(successResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(true));
      assertThat(junit.constructed().size(), is(1));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
    }
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifyFail(final TestCase first, final TestCase second)
      throws ClassNotFoundException {
    final Result failResult = mock(Result.class);
    when(failResult.wasSuccessful()).thenReturn(false);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              when(mock.run(any(Request.class))).thenReturn(failResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(false));
      assertThat(junit.constructed().size(), is(1));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
    }
  }
}
