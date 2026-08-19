package moira.util.cli;

import java.util.concurrent.Callable;
import moira.util.factory.DefaultFactory;
import moira.util.factory.MoiraFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "moira",
    subcommands = {DetectCommand.class, ListCommand.class, VerifyCommand.class, HelpCommand.class},
    description = "A tool to detect dependencies between tests of a testsuite.",
    version = "moira 0.0.1",
    usageHelpAutoWidth = true)
public class MoiraUtil implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Option(
      description = "Display help and exit.",
      names = {"--help", "-h"},
      usageHelp = true)
  private boolean help;

  @Option(
      description = "Display version and exit.",
      names = {"--version", "-V"},
      versionHelp = true)
  private boolean version;

  private final MoiraFactory factory;

  public MoiraUtil(final MoiraFactory factory) {
    this.factory = factory;
  }

  public MoiraFactory factory() {
    return factory;
  }

  @Override
  public Integer call() {
    spec.commandLine().usage(spec.commandLine().getErr());
    return 1;
  }

  public static void main(final String... args) {
    final MoiraUtil moira = new MoiraUtil(new DefaultFactory());
    final int exitCode = new CommandLine(moira).execute(args);

    System.exit(exitCode);
  }
}
