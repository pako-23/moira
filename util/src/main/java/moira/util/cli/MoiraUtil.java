package moira.util.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "moira",
    subcommands = {ListCommand.class, VerifyCommand.class, TuscanCommand.class, HelpCommand.class},
    version = "moira 0.0.1",
    usageHelpAutoWidth = true)
public class MoiraUtil implements Callable<Integer> {
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
    int exitCode = new CommandLine(new MoiraUtil()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    spec.commandLine().usage(spec.commandLine().getOut());
    return 1;
  }
}
