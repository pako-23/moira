package moira.util.tuscan;

import static moira.util.tuscan.PairCoverMatcher.*;
import static moira.util.tuscan.PossibleBrittleMatcher.*;
import static moira.util.tuscan.TuscanAllPairsMatcher.*;
import static moira.util.tuscan.TuscanClassOnlyMatcher.*;
import static moira.util.tuscan.TuscanIntraClassMatcher.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import moira.util.model.Range;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ScheduleGeneratorsTest {
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

  @ParameterizedTest
  @MethodSource("testSuiteOrdersWithSeeds")
  public void testTargetPairsCoversAllRandomPairs(final Class<?>[] order, final int seed) {
    mockTestSuite(order);
    final Map<TestCase, Set<TestCase>> pairs = generateRandomPairs(seed);

    assertThat(new TargetPairsGenerator(pairs), coversAllPairs(pairs));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrdersWithSeeds")
  public void testPairCoverCoversAllRandomPairs(final Class<?>[] order, final int seed) {
    mockTestSuite(order);
    final Map<TestCase, Set<TestCase>> pairs = generateRandomPairs(seed);

    assertThat(new PairCover(pairs), coversAllPairs(pairs));
  }

  @ParameterizedTest
  @MethodSource("testSuiteOrdersWithSeeds")
  public void testPairCoverCoversPossibleBrittleForRandomPairs(
      final Class<?>[] order, final int seed) {
    mockTestSuite(order);
    final Map<TestCase, Set<TestCase>> pairs = generateRandomPairs(seed);

    assertThat(new PairCover(pairs), coversPossibleBrittleTests(pairs));
  }

  private void mockTestSuite(final Class<?>[] order) {
    int testCases = 0;

    when(suite.numberOfTestClasses()).thenReturn(order.length);

    for (int i = 0; i < order.length; ++i) {
      int index = 0;
      for (; index < classes.length; ++index) if (order[i].equals(classes[index])) break;
      doReturn(classes[index].getName()).when(suite).getTestClass(i);
      when(suite.getTestClassCases(classes[index].getName()))
          .thenReturn(new Range(testCases, testCases + rangeSizes[index]));
      for (int j = 0; j < rangeSizes[index]; ++j) {
        final TestCase testCase = mock(TestCase.class);
        doReturn(classes[index].getName()).when(testCase).getTestClass();
        when(suite.getTestCase(testCases)).thenReturn(testCase);
        ++testCases;
      }
    }

    when(suite.numberOfTestCases()).thenReturn(testCases);
  }

  private Map<TestCase, Set<TestCase>> generateRandomPairs(final int seed) {
    final Map<TestCase, Set<TestCase>> pairs = new HashMap<>();
    final Random random = new Random(seed);
    final int n = suite.numberOfTestCases();

    if (n == 0) return pairs;

    for (int i = 0; i < n; ++i) {
      final Set<TestCase> targets = new HashSet<>();
      for (int j = 0; j < n; ++j) {
        if (i != j && random.nextBoolean()) targets.add(suite.getTestCase(j));
      }
      if (!targets.isEmpty()) pairs.put(suite.getTestCase(i), targets);
    }

    return pairs;
  }

  private static Stream<Arguments> testSuiteOrdersWithSeeds() {
    return testSuiteOrders()
        .flatMap(
            args -> {
              return IntStream.of(0, 1, 3, 10, 11, 12, 42, 100, 120)
                  .mapToObj(seed -> Arguments.of(args.get()[0], seed));
            });
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
