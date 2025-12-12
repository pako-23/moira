package moira.util.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "moira.util.cli.MoiraUtil",
    subcommands = {VerifyCommand.class, HelpCommand.class},
    version = "MoiraUtil 0.1",
    usageHelpAutoWidth = true)
public class MoiraUtil implements Runnable {
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
    new CommandLine(new MoiraUtil()).execute(args);
  }

  @Override
  public void run() {
    spec.commandLine().usage(System.out);
  }
}
