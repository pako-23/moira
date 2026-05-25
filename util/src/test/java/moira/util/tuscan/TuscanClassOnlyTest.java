package moira.util.tuscan;

import static moira.util.tuscan.TuscanClassOnlyMatcher.*;
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

public class TuscanClassOnlyTest {
  private static final Class<?>[] classes = {
    Integer.class, String.class, Boolean.class,
    Double.class, Long.class, Map.class,
  };
  private static final int[] rangeSizes = {0, 0, 2, 10, 13, 23};

  @Mock private TestSuite suite;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @ParameterizedTest
  @MethodSource("testClassOrders")
  public void testTuscanClassOnlyConstruction(final int[] order) {
    mockTestSuite(order);

    assertThat(new TuscanClassOnly(suite), isTuscanClassOnlySquare(suite));
  }

  private void mockTestSuite(final int[] order) {
    int testCases = 0;

    when(suite.numberOfTestClasses()).thenReturn(order.length);

    for (int i = 0; i < order.length; ++i) {
      int index = order[i];
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

  private static Stream<Arguments> testClassOrders() {
    return Stream.of(
        Arguments.of(new int[] {2}),
        Arguments.of(new int[] {2, 3}),
        Arguments.of(new int[] {4}),
        Arguments.of(new int[] {4, 5}),
        Arguments.of(new int[] {0}),
        Arguments.of(new int[] {}),
        Arguments.of(new int[] {2, 0, 1, 4, 3, 5}));
  }
}
