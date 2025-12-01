package ch.usi.inf.runner;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

public class ScheduleExecutor {
  public static void main(String... args) {
    if (args.length == 0) {
      printHelp();
      System.exit(1);
    } else if (args[0].equals("--discover") || args[0].equals("-d")) {
      discoverTests(args);
    } else if (args[0].equals("--help") || args[0].equals("-h")) {
      printHelp();
    } else {
      executeSchedule(args);
    }
  }

  private static void printHelp() {
    System.out.println("Usage: [options] [tests...]\n");
    System.out.println("Execute the given test schedule in the given order.\n");
    System.out.println("Options:");
    System.out.println("  -d <directory>, --discovery <direcory>");
    System.out.println("                  Discover all the tests in the given diretory");
    System.out.println("  -h, --help");
    System.out.println("                  Display this help and exit");
  }

  private static void discoverTests(String... args) {
    if (args.length != 2) {
      System.err.println(
          "The --discover option requires a single directory containing test java class files");
      printHelp();
      System.exit(1);
    }

    File directory = new File(args[1]);
    if (!directory.exists()) {
      System.err.println("The given path does not exist");
      System.exit(1);
    } else if (!directory.isDirectory()) {
      System.err.println("The given path should be a directory containing java class files");
      System.exit(1);
    }

    TestsFinder finder = new TestsFinder(directory);
    finder.getTests().stream().forEach(System.out::println);
  }

  private static void executeSchedule(String... args) {
    Runner runner = new ScheduleRunner(args);
    Result result = new Result();
    RunListener listener = result.createListener();
    ScheduleListener scheduleListener = new ScheduleListener(args);
    final RunNotifier notifier = new RunNotifier();
    PrintStream originalStdout = System.out;
    PrintStream originalStderr = System.err;
    PrintStream dumbStream =
        new PrintStream(
            new OutputStream() {
              public void write(int b) {}
            });

    System.setOut(dumbStream);
    System.setErr(dumbStream);

    notifier.addFirstListener(listener);
    notifier.addListener(scheduleListener);

    notifier.fireTestRunStarted(runner.getDescription());
    runner.run(notifier);
    notifier.fireTestRunFinished(result);

    System.setOut(originalStdout);
    System.setErr(originalStderr);

    System.out.println(
        Arrays.stream(scheduleListener.getTestsOutcome()).collect(Collectors.joining(" ")));

    System.setOut(dumbStream);
    System.setErr(dumbStream);

    System.exit(0);
  }
}
