package moira.util.junit;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import moira.util.model.Outcome;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleRun;
import moira.util.runner.TestRunner;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.InitializationError;

public class JUnitRunner implements TestRunner {
  @Override
  public ScheduleRun request(final String... testClasses) {
    final Class<?>[] classes =
        Stream.of(testClasses)
            .map(
                className -> {
                  try {
                    return Class.forName(className);
                  } catch (final ClassNotFoundException e) {
                    return null;
                  }
                })
            .filter(clazz -> clazz != null)
            .toArray(Class<?>[]::new);

    try {
      final JUnitRunnerBuilder builder = new JUnitRunnerBuilder();
      final Computer computer = new JUnitComputer();
      final Runner suite = computer.getSuite(builder, classes);

      return new JUnitScheduleRun(runner(suite));
    } catch (final InitializationError e) {
      throw new RuntimeException("failed to request schedule run: " + e.getCauses());
    }
  }

  private static Request runner(final Runner runner) {
    return new Request() {
      @Override
      public Runner getRunner() {
        return runner;
      }
    };
  }

  private static class JUnitScheduleRun implements ScheduleRun {
    private Request request;

    public JUnitScheduleRun(final Request request) {
      this.request = request;
    }

    @Override
    public List<Outcome> run() {
      final JUnitCore junit = new JUnitCore();
      final JUnitResultsCollector listener = new JUnitResultsCollector();

      junit.addListener(listener);
      junit.run(request);

      return listener.getOutcomes();
    }

    @Override
    public ScheduleRun withFilter(final Predicate<TestCase> predicate) {
      final Runner baseRunner = request.getRunner();
      final Filter filter =
          new Filter() {
            @Override
            public String describe() {
              return "junit schedule run filter";
            }

            @Override
            public boolean shouldRun(final Description description) {
              if (description.isSuite()) return true;

              return predicate.test(JUnitDescription.convert(description));
            }
          };

      try {
        filter.apply(baseRunner);
      } catch (final NoTestsRemainException e) {
        throw new RuntimeException(filter.describe() + " removed all tests", e);
      }

      request =
          new Request() {
            @Override
            public Runner getRunner() {
              return baseRunner;
            }
          };

      return this;
    }

    @Override
    public ScheduleRun withOrder(final Comparator<TestCase> comparator) {
      request =
          request.sortWith(
              (a, b) -> {
                if (a.isSuite()) return 0;
                return comparator.compare(JUnitDescription.convert(a), JUnitDescription.convert(b));
              });

      return this;
    }
  }
}
