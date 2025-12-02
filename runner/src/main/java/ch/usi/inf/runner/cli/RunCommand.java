package ch.usi.inf.runner.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "run",
    description = "Run a series of tests in the given order.",
    usageHelpAutoWidth = true)
public class RunCommand implements Runnable {
  @ParentCommand private JUnitLauncher parent;

  @Parameters(paramLabel = "<test>", description = "The tests schedule to run.", arity = "1..*")
  private String[] schedule;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Override
  public void run() {
    System.out.println("TODO: implement run command");
  }
}
