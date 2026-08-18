package moira.util.junit;

import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runners.model.InitializationError;

public class ScheduleRun {

  private Request request;

  public ScheduleRun(final Class<?>... classes) {
    try {
      final AllDefaultPossibilitiesBuilder builder = new AllDefaultPossibilitiesBuilder();
      final Computer computer = new Computer();
      final Runner suite = computer.getSuite(builder, classes);

      request = runner(suite);
    } catch (final InitializationError e) {
      request = runner(new ErrorReportingRunner(e, classes));
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

  public ScheduleRun withFilter(final Filter filter) {
    request = request.filterWith(filter);
    return this;
  }

  public void run() {
    final JUnitCore junit = new JUnitCore();

    junit.run(request);
  }
}
