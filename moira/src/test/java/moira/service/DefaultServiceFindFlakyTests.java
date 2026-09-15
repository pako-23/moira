package moira.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import moira.collector.FlakyPairsCollector;
import moira.model.Outcome;
import moira.model.TestCase;
import moira.schedules.ScheduleGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

public class DefaultServiceFindFlakyTests extends DefaultServiceTest {

  private static final TestCase TEST_1 = TestCase.fromId("com.example.ExampleTest[desc1]");
  private static final TestCase TEST_2 = TestCase.fromId("com.example.ExampleTest[desc2]");
  private static final TestCase TEST_3 = TestCase.fromId("com.example.ExampleTest2[desc]");
  private static final TestCase TEST_4 = TestCase.fromId("com.example.ExampleTest3[desc]");

  @Mock private ScheduleGenerator generator;

  @ParameterizedTest
  @MethodSource("provideOutcomes")
  public void testSchedulesExecution(final Outcome[][] outcomes) throws IOException {
    final TestCase[][] schedules =
        Stream.of(outcomes)
            .map(outcome -> Stream.of(outcome).map(Outcome::testCase).toArray(TestCase[]::new))
            .toArray(TestCase[][]::new);

    generatorRetunsSchedules(schedules);

    final RecordingCollector collector = new RecordingCollector();
    final MockedExecution[] executions = captureExecutions(schedules.length);
    final Logger logger = mock(Logger.class);

    for (int i = 0; i < outcomes.length; ++i)
      executions[i].writeStdOutLines(
          Stream.of(outcomes[i])
              .map(Outcome::pass)
              .map(pass -> pass ? "true" : "false")
              .toArray(String[]::new));

    service.setLogger(logger);
    service.findFlakyPairs(generator, collector);

    for (final MockedExecution execution : executions)
      assertThat(execution.getArguments(), hasItem(moira.service.ScheduleExecutor.class.getName()));

    assertThat(collector.updates.size(), is(outcomes.length));
    for (int i = 0; i < outcomes.length; ++i) {
      final Outcome[] update = collector.updates.get(i);

      assertThat(update.length, is(outcomes[i].length));
      assertThat(
          Stream.of(update).map(Outcome::pass).collect(Collectors.toList()),
          contains(Stream.of(outcomes[i]).map(Outcome::pass).toArray(Boolean[]::new)));
    }

    for (int i = 0; i <= generator.count(); ++i)
      verify(logger).log(String.format("progress %d/%d", i, generator.count()));

    for (int i = 0; i < outcomes.length; ++i)
      assertThat(
          Arrays.asList(executions[i].getStdInContent().trim().split("\\n")),
          contains(Stream.of(schedules[i]).map(TestCase::toString).toArray(String[]::new)));
  }

  @Test
  public void testInvalidLinesInExecution() {
    final RecordingCollector collector = new RecordingCollector();
    final MockedExecution[] executions = captureExecutions(1);

    generatorRetunsSchedules(new TestCase[][] {{TEST_1, TEST_2}});

    executions[0].writeStdOutLines("hello", "true", "garbage", "false", "", "TRUE");

    service.findFlakyPairs(generator, collector);

    assertThat(collector.updates.size(), is(1));
    assertThat(
        Stream.of(collector.updates.get(0)).map(Outcome::pass).collect(Collectors.toList()),
        contains(true, false));
  }

  @Test
  public void testMoreOutcomesThanScheduleLengthThrows() {
    final MockedExecution[] executions = captureExecutions(1);

    generatorRetunsSchedules(new TestCase[][] {{TEST_1, TEST_2}});

    executions[0].writeStdOutLines("true", "true", "true");

    final RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> service.findFlakyPairs(generator, new RecordingCollector()));
    assertThat(
        exception.getMessage(), containsString("got 3 outcomes from a schedule of length 2"));
  }

  private static Stream<Arguments> provideOutcomes() {
    return Stream.of(
        Arguments.of((Object) new Outcome[][] {}),
        Arguments.of(
            (Object)
                new Outcome[][] {
                  new Outcome[] {new Outcome(TEST_1, true), new Outcome(TEST_2, true)},
                  new Outcome[] {new Outcome(TEST_3, true)},
                  new Outcome[] {
                    new Outcome(TEST_2, true), new Outcome(TEST_3, false), new Outcome(TEST_4, true)
                  },
                }),
        Arguments.of(
            (Object)
                new Outcome[][] {
                  new Outcome[] {new Outcome(TEST_1, true), new Outcome(TEST_2, false)},
                  new Outcome[] {
                    new Outcome(TEST_4, false),
                    new Outcome(TEST_1, false),
                    new Outcome(TEST_3, true)
                  },
                }),
        Arguments.of(
            (Object)
                new Outcome[][] {
                  new Outcome[] {new Outcome(TEST_3, true), new Outcome(TEST_2, false)},
                  new Outcome[] {new Outcome(TEST_3, false), new Outcome(TEST_4, true)},
                }));
  }

  private void generatorRetunsSchedules(final TestCase[][] schedules) {
    when(generator.count()).thenReturn(schedules.length);
    if (schedules.length > 0)
      when(generator.generate())
          .thenReturn(schedules[0], Stream.of(schedules).skip(1).toArray(TestCase[][]::new));
  }

  private static final class RecordingCollector extends FlakyPairsCollector {
    private final List<Outcome[]> updates = new ArrayList<>();

    @Override
    public void update(final Outcome[] outcome) {
      updates.add(outcome);
    }
  }
}
