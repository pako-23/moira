package moira.util.tuscan;

import static moira.util.tuscan.TuscanAllPairsMatcher.*;
import static moira.util.tuscan.TuscanClassOnlyMatcher.*;
import static moira.util.tuscan.TuscanIntraClassMatcher.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.stream.Stream;
import moira.util.Range;
import moira.util.TestCase;
import moira.util.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TuscanSchedulesGeneratorTest {
  private static final Class<?>[] classes = {
    Integer.class, String.class, Boolean.class,
    Double.class, Long.class, Map.class,
  };
  private static final int[] rangeSizes = {0, 0, 2, 6, 5, 13};

  @Mock private TestSuite suite;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanClassOnlyConstruction(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanClassOnly(suite), isTuscanClassOnlySquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanPackedIsClassOnlySquare(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanPacked(suite), isTuscanClassOnlySquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanPackedIsIntraClassSquare(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanPacked(suite), isTuscanIntraClassSquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanPackedHasAllPairs(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanPacked(suite), isAllPairsTuscanSquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanIntraClassIsClassOnlySquare(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanIntraClass(suite), isTuscanClassOnlySquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanIntraClassConstruction(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanIntraClass(suite), isTuscanIntraClassSquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanInterClassIsClassOnlySquare(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanInterClass(suite), isTuscanClassOnlySquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanInterClassIsIntraClassSquare(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanInterClass(suite), isTuscanIntraClassSquare(suite));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrders")
  public void testTuscanInterClassHasAllPairs(final Class<?>[] order) {
    mockTestSuite(order);

    assertThat(new TuscanInterClass(suite), isAllPairsTuscanSquare(suite));
  }

  private void mockTestSuite(final Class<?>[] order) {
    int testCases = 0;

    when(suite.numberOfTestClasses()).thenReturn(order.length);

    for (int i = 0; i < order.length; ++i) {
      int index = 0;
      for (; index < classes.length; ++index) if (order[i].equals(classes[index])) break;
      doReturn(classes[index]).when(suite).getTestClass(i);
      when(suite.getTestClassCases(classes[index]))
          .thenReturn(new Range(testCases, testCases + rangeSizes[index]));
      for (int j = 0; j < rangeSizes[index]; ++j) {
        final TestCase testCase = mock(TestCase.class);
        doReturn(classes[index]).when(testCase).getTestClass();
        when(suite.getTestCase(testCases)).thenReturn(testCase);
        ++testCases;
      }
    }

    when(suite.numberOfTestCases()).thenReturn(testCases);
  }

  private static Stream<Arguments> testSuiteOrders() {
    return Stream.of(
        Arguments.of((Object) new Class<?>[] {Boolean.class}),
        Arguments.of((Object) new Class<?>[] {Boolean.class, Double.class}),
        Arguments.of((Object) new Class<?>[] {Long.class}),
        Arguments.of((Object) new Class<?>[] {Long.class, Map.class}),
        Arguments.of((Object) new Class<?>[] {}),
        Arguments.of((Object) new Class<?>[] {Long.class, Map.class, String.class}),
        Arguments.of(
            (Object)
                new Class<?>[] {
                  Boolean.class, Integer.class, Double.class, Long.class, String.class, Map.class
                }),
        Arguments.of(
            (Object)
                new Class<?>[] {
                  Boolean.class, Integer.class, Long.class, Double.class, Map.class
                }));
  }
}
