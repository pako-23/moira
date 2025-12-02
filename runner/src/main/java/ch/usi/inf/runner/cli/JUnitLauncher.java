package ch.usi.inf.runner.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "ch.usi.inf.runner.cli.JUnitLauncher",
    subcommands = {ListCommand.class, RunCommand.class, HelpCommand.class},
    version = "JUnitLauncher 0.1",
    usageHelpAutoWidth = true)
public class JUnitLauncher implements Runnable {
  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Option(
      names = {"-V", "-version"},
      versionHelp = true,
      description = "Display version and exit.")
  private boolean version;

  @Spec private CommandSpec spec;

  public static void main(final String[] args) {
    int exitCode = new CommandLine(new JUnitLauncher()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    spec.commandLine().usage(System.out);
  }
}
