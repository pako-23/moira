package moira.util.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import moira.util.FlakyPairsCollector;
import moira.util.PairsCollector;
import moira.util.TuscanSquareCollector;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import moira.util.model.TestSuite;
import moira.util.runner.ScheduleGenerator;
import moira.util.service.DefaultService;
import moira.util.service.Service;
import moira.util.tuscan.PairCover;
import moira.util.tuscan.TargetPairsGenerator;
import moira.util.tuscan.TuscanClassOnly;
import moira.util.tuscan.TuscanInterClass;
import moira.util.tuscan.TuscanIntraClass;
import moira.util.tuscan.TuscanPacked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultFactoryTest {

  private static final TestCase TEST_1 = TestCase.fromId("com.example.ExampleTest[desc1]");
  private static final TestCase TEST_2 = TestCase.fromId("com.example.ExampleTest[desc2]");
  private static final TestCase TEST_3 = TestCase.fromId("com.example.ExampleTest[desc3]");
  private static final TestCase TEST_4 = TestCase.fromId("com.example.ExampleTest2[desc1]");
  private static final TestCase TEST_5 = TestCase.fromId("com.example.ExampleTest2[desc2]");

  private static final String SINGLE_PAIR = pairLine(TEST_1, TEST_4);
  private static final String[] MULTIPLE_PAIRS = {
    pairLine(TEST_1, TEST_4),
    pairLine(TEST_1, TEST_3),
    pairLine(TEST_2, TEST_5),
    pairLine(TEST_4, TEST_5),
  };
  private static final String[] MALFORMED_LINES = {
    "",
    "garbage without separator",
    "from: " + TEST_1 + ", to:      " + TEST_4 + "   ",
    "from: " + TEST_1,
  };

  private MoiraFactory factory;
  @Mock private Service service;

  @BeforeEach
  public void setup() {
    factory = new DefaultFactory();
  }

  @Test
  public void testCreateServiceReturnsDefaultService() {
    final Service service = factory.createService();

    assertThat(service, notNullValue());
    assertThat(service, instanceOf(DefaultService.class));
  }

  @ParameterizedTest
  @MethodSource("tuscanGenerators")
  public void testCreateScheduleGeneratorReturnsTuscanGenerator(
      final DetectionMode mode, final Class<? extends ScheduleGenerator> expectedClass)
      throws IOException {
    final File source = createTempSourceFile();

    when(service.discoverTestSuite(source)).thenReturn(new TestSuite(new ArrayList<>()));
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator, notNullValue());
    assertThat(generator, instanceOf(expectedClass));
  }

  @ParameterizedTest
  @MethodSource("pairsGenerators")
  public void testCreateScheduleGeneratorReturnsPairsGenerator(
      final DetectionMode mode, final Class<? extends ScheduleGenerator> expectedClass)
      throws IOException {
    final File source = createTempSourceFile(SINGLE_PAIR);
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator, notNullValue());
    assertThat(generator, instanceOf(expectedClass));
  }

  @ParameterizedTest
  @MethodSource("pairsModes")
  public void testCreateScheduleGeneratorEmptyFile(final DetectionMode mode) throws IOException {
    final File source = createTempSourceFile();
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator.count(), is(0));
  }

  @ParameterizedTest
  @MethodSource("pairsModes")
  public void testCreateScheduleGeneratorSinglePair(final DetectionMode mode) throws IOException {
    final File source = createTempSourceFile(SINGLE_PAIR);
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator.count(), greaterThan(0));
  }

  @ParameterizedTest
  @MethodSource("pairsModes")
  public void testCreateScheduleGeneratorMultiplePairs(final DetectionMode mode)
      throws IOException {
    final File source = createTempSourceFile(MULTIPLE_PAIRS);
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator.count(), greaterThan(0));
  }

  @ParameterizedTest
  @MethodSource("pairsModes")
  public void testCreateScheduleGeneratorMalformedLines(final DetectionMode mode)
      throws IOException {
    final File source = createTempSourceFile(MALFORMED_LINES);
    final ScheduleGenerator generator = factory.createScheduleGenerator(mode, service, source);

    assertThat(generator.count(), greaterThan(0));
  }

  @ParameterizedTest
  @MethodSource("tuscanModes")
  public void testCreateFlakyPairsCollectorReturnsTuscanSquareCollector(final DetectionMode mode) {
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(mode, new File("testsuite"));

    assertThat(collector, notNullValue());
    assertThat(collector, instanceOf(TuscanSquareCollector.class));
  }

  @ParameterizedTest
  @MethodSource("pairsModes")
  public void testCreateFlakyPairsCollectorReturnsPairsCollector(final DetectionMode mode)
      throws IOException {
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(mode, createTempSourceFile());

    assertThat(collector, notNullValue());
    assertThat(collector, instanceOf(PairsCollector.class));
  }

  @Test
  public void testCreateFlakyPairsCollectorEmptyFile() throws IOException {
    final File source = createTempSourceFile();
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(DetectionMode.MOIRA, source);

    assertThat(capturePrint(collector), emptyString());
  }

  @Test
  public void testCreateFlakyPairsCollectorSinglePair() throws IOException {
    final File source = createTempSourceFile(SINGLE_PAIR);
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(DetectionMode.MOIRA, source);

    collector.update(new Outcome[] {new Outcome(TEST_1, true), new Outcome(TEST_4, false)});

    assertThat(capturePrint(collector), containsString(SINGLE_PAIR));
  }

  @Test
  public void testCreateFlakyPairsCollectorMultiplePairs() throws IOException {
    final File source = createTempSourceFile(MULTIPLE_PAIRS);
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(DetectionMode.MOIRA, source);

    collector.update(
        new Outcome[] {
          new Outcome(TEST_1, true), new Outcome(TEST_4, false), new Outcome(TEST_5, false),
        });
    collector.update(
        new Outcome[] {
          new Outcome(TEST_1, true),
          new Outcome(TEST_3, false),
          new Outcome(TEST_2, true),
          new Outcome(TEST_5, false),
        });

    final String output = capturePrint(collector);
    for (final String line : MULTIPLE_PAIRS) assertThat(output, containsString(line));
  }

  @Test
  public void testCreateFlakyPairsCollectorPairNotInFileIsIgnored() throws IOException {
    final File source = createTempSourceFile(pairLine(TEST_1, TEST_3));
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(DetectionMode.TARGET_PAIRS, source);

    collector.update(new Outcome[] {new Outcome(TEST_1, true), new Outcome(TEST_4, false)});

    assertThat(capturePrint(collector), emptyString());
  }

  @Test
  public void testCreateFlakyPairsCollectorSkipsMalformedLines() throws IOException {
    final File source = createTempSourceFile(MALFORMED_LINES);
    final FlakyPairsCollector collector =
        factory.createFlakyPairsCollector(DetectionMode.TARGET_PAIRS, source);

    collector.update(new Outcome[] {new Outcome(TEST_1, true), new Outcome(TEST_4, false)});

    final String output = capturePrint(collector);
    assertThat(output, containsString(SINGLE_PAIR));
  }

  @Test
  public void testMissingPairsFileThrows() {
    final File missing = new File("not-existing");

    assertMissingPairsFile(mode -> factory.createScheduleGenerator(mode, service, missing));
    assertMissingPairsFile(mode -> factory.createFlakyPairsCollector(mode, missing));
  }

  private static void assertMissingPairsFile(final Function<DetectionMode, ?> create) {
    for (final DetectionMode mode :
        new DetectionMode[] {DetectionMode.TARGET_PAIRS, DetectionMode.MOIRA}) {
      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> create.apply(mode));
      assertThat(exception.getMessage(), containsString("failed to read pairs file: "));
    }
  }

  private static String pairLine(final TestCase from, final TestCase to) {
    return String.format("from: %s, to: %s", from, to);
  }

  private static File createTempSourceFile(final String... lines) throws IOException {
    final File source = File.createTempFile("default-factory-", ".txt");
    source.deleteOnExit();

    Files.write(source.toPath(), Arrays.asList(lines));

    return source;
  }

  private static String capturePrint(final FlakyPairsCollector collector) {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (final PrintWriter stream = new PrintWriter(buffer)) {
      collector.print(stream);
    }
    return buffer.toString();
  }

  private static Stream<DetectionMode> tuscanModes() {
    return Stream.of(
        DetectionMode.TUSCAN_CLASS_ONLY,
        DetectionMode.TUSCAN_INTER_CLASS,
        DetectionMode.TUSCAN_INTRA_CLASS,
        DetectionMode.TUSCAN_PACKED);
  }

  private static Stream<DetectionMode> pairsModes() {
    return Stream.of(DetectionMode.TARGET_PAIRS, DetectionMode.MOIRA);
  }

  private static Stream<Arguments> tuscanGenerators() {
    return Stream.of(
        Arguments.of(DetectionMode.TUSCAN_CLASS_ONLY, TuscanClassOnly.class),
        Arguments.of(DetectionMode.TUSCAN_INTRA_CLASS, TuscanIntraClass.class),
        Arguments.of(DetectionMode.TUSCAN_INTER_CLASS, TuscanInterClass.class),
        Arguments.of(DetectionMode.TUSCAN_PACKED, TuscanPacked.class));
  }

  private static Stream<Arguments> pairsGenerators() {
    return Stream.of(
        Arguments.of(DetectionMode.TARGET_PAIRS, TargetPairsGenerator.class),
        Arguments.of(DetectionMode.MOIRA, PairCover.class));
  }
}
