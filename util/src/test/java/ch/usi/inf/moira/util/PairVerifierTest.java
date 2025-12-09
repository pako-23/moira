package ch.usi.inf.moira.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
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
  private static final TestMethod firstTest =
      new TestMethod(
          PairVerifierTest.class.getName() + "[first(" + PairVerifierTest.class.getName() + ")]");
  private static final TestMethod secondTest =
      new TestMethod(
          PairVerifierTest.class.getName() + "[second(" + PairVerifierTest.class.getName() + ")]");
  private static final TestMethod otherTest =
      new TestMethod(
          PairVerifier.class.getName() + "[sometest(" + PairVerifier.class.getName() + ")]");

  @Test
  public void testSingleClassTestSchedules() {
    final ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
    @SuppressWarnings("unchecked")
    final ArgumentCaptor<Comparator<Description>> comparatorCaptor =
        ArgumentCaptor.forClass(Comparator.class);

    try (final MockedStatic<Request> requestMock = mockStatic(Request.class)) {
      final Request returnedRequest = mock(Request.class);
      when(returnedRequest.filterWith(filterCaptor.capture())).thenReturn(returnedRequest);
      when(returnedRequest.sortWith(comparatorCaptor.capture())).thenReturn(returnedRequest);
      requestMock.when(() -> Request.aClass(firstTest.getTestClass())).thenReturn(returnedRequest);

      new PairVerifier(firstTest, secondTest);
      final List<Filter> filters = filterCaptor.getAllValues();
      final List<Comparator<Description>> comparators = comparatorCaptor.getAllValues();

      final Description firstDescription =
          Description.createTestDescription(firstTest.getTestClass(), "first");
      final Description secondDescription =
          Description.createTestDescription(firstTest.getTestClass(), "second");

      assertThat(filters.size(), is(2));
      for (final Filter filter : filters) {
        assertThat(filter.describe(), is("pair filter"));

        assertThat(filter.shouldRun(firstDescription), is(true));
        assertThat(filter.shouldRun(secondDescription), is(true));
        assertThat(
            filter.shouldRun(Description.createTestDescription(getClass(), "third")), is(false));
        assertThat(
            filter.shouldRun(Description.createTestDescription(PairVerifier.class, "first")),
            is(false));
      }

      assertThat(comparators.size(), is(2));
      assertThat(comparators.get(0).compare(firstDescription, secondDescription), greaterThan(0));
      assertThat(comparators.get(0).compare(secondDescription, firstDescription), lessThan(0));
      assertThat(comparators.get(1).compare(secondDescription, firstDescription), greaterThan(0));
      assertThat(comparators.get(1).compare(firstDescription, secondDescription), lessThan(0));
    }
  }

  @Test
  public void testTwoClassesTestSchedules() {
    final ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);

    try (final MockedStatic<Request> requestMock = mockStatic(Request.class)) {
      final Request returnedRequest = mock(Request.class);
      when(returnedRequest.filterWith(filterCaptor.capture())).thenReturn(returnedRequest);
      requestMock
          .when(() -> Request.classes(otherTest.getTestClass(), firstTest.getTestClass()))
          .thenReturn(returnedRequest);
      requestMock
          .when(() -> Request.classes(firstTest.getTestClass(), otherTest.getTestClass()))
          .thenReturn(returnedRequest);

      new PairVerifier(firstTest, otherTest);
      final List<Filter> filters = filterCaptor.getAllValues();

      final Description firstDescription =
          Description.createTestDescription(firstTest.getTestClass(), "first");
      final Description secondDescription =
          Description.createTestDescription(otherTest.getTestClass(), "sometest");

      assertThat(filters.size(), is(2));
      for (final Filter filter : filters) {
        assertThat(filter.describe(), is("pair filter"));

        final Description firstSuite = Description.createSuiteDescription(firstTest.getTestClass());
        final Description secondSuite =
            Description.createSuiteDescription(otherTest.getTestClass());

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
  }

  private static Stream<Arguments> testVerifyParams() {
    return Stream.of(Arguments.of(firstTest, secondTest), Arguments.of(firstTest, otherTest));
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifyPassBoth(final TestMethod first, final TestMethod second) {
    final Result successResult = mock(Result.class);
    when(successResult.wasSuccessful()).thenReturn(true);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              when(mock.run(any(Request.class))).thenReturn(successResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(false));
      assertThat(junit.constructed().size(), is(2));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
      verify(junit.constructed().get(1), times(1)).run(any(Request.class));
    }
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifysFailBoth(final TestMethod first, final TestMethod second) {
    final Result successResult = mock(Result.class);
    when(successResult.wasSuccessful()).thenReturn(true);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              when(mock.run(any(Request.class))).thenReturn(successResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(false));
      assertThat(junit.constructed().size(), is(2));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
      verify(junit.constructed().get(1), times(1)).run(any(Request.class));
    }
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifyFailPass(final TestMethod first, final TestMethod second) {
    final Result successResult = mock(Result.class);
    when(successResult.wasSuccessful()).thenReturn(true);

    final Result failResult = mock(Result.class);
    when(failResult.wasSuccessful()).thenReturn(false);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              if (context.getCount() == 1)
                when(mock.run(any(Request.class))).thenReturn(failResult);
              else when(mock.run(any(Request.class))).thenReturn(successResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(true));
      assertThat(junit.constructed().size(), is(2));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
      verify(junit.constructed().get(1), times(1)).run(any(Request.class));
    }
  }

  @ParameterizedTest
  @MethodSource("testVerifyParams")
  public void testVerifyPassFail(final TestMethod first, final TestMethod second) {
    final Result successResult = mock(Result.class);
    when(successResult.wasSuccessful()).thenReturn(true);

    final Result failResult = mock(Result.class);
    when(failResult.wasSuccessful()).thenReturn(false);

    try (final MockedConstruction<JUnitCore> junit =
        mockConstruction(
            JUnitCore.class,
            (mock, context) -> {
              if (context.getCount() == 1)
                when(mock.run(any(Request.class))).thenReturn(successResult);
              else when(mock.run(any(Request.class))).thenReturn(failResult);
            })) {

      final PairVerifier verifier = new PairVerifier(first, second);
      assertThat(verifier.verify(), is(true));
      assertThat(junit.constructed().size(), is(2));
      verify(junit.constructed().get(0), times(1)).run(any(Request.class));
      verify(junit.constructed().get(1), times(1)).run(any(Request.class));
    }
  }
}
