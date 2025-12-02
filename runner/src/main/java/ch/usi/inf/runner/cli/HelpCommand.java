package ch.usi.inf.runner.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "help",
    description = "Display help information about a specific command.",
    usageHelpAutoWidth = true)
public class HelpCommand implements Runnable {

  @Parameters(
      paramLabel = "COMMAND",
      description = "The command whose help message to display.",
      arity = "1")
  private String commandName;

  @Option(
      names = {"-h", "-help"},
      usageHelp = true,
      description = "Display this help and exit.")
  private boolean help;

  @Override
  public void run() {
    final CommandLine main = new CommandLine(new JUnitLauncher());
    final CommandLine command = main.getSubcommands().get(commandName);
    if (command == null) {
      System.out.printf("Unknown command: '%s'\n", commandName);
      main.usage(System.out);
      return;
    }

    command.usage(System.out);
  }
}
