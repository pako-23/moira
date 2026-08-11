package moira.util.cli;

import java.util.concurrent.Callable;
import moira.util.service.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
    name = "moira",
    subcommands = {ListCommand.class, VerifyCommand.class, HelpCommand.class},
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

  private final Service service;

  public MoiraUtil(final Service service) {
    this.service = service;
  }

  public Service service() {
    return service;
  }

  @Override
  public Integer call() {
    spec.commandLine().usage(spec.commandLine().getErr());
    return 1;
  }
}
