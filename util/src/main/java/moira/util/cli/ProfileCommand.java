package moira.util.cli;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import moira.util.factory.MoiraFactory;
import moira.util.model.TestCase;
import moira.util.service.Profiler;
import moira.util.service.Service;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.TypeConversionException;

@Command(
    name = "profile",
    description = "Run profiler on the given testsuite to detect flaky tests.",
    usageHelpAutoWidth = true)
public class ProfileCommand implements Callable<Integer> {
  @ParentCommand private MoiraUtil parent;
  @Spec private CommandSpec spec;

  @Parameters(
      paramLabel = "<testsuite>",
      description = "The path to a file containing the testsuite.")
  private File testsuite;

  @Option(
      converter = ProfilerConverter.class,
      description =
          "The profiler implementation. Valid values are: online, naive, object, target-pairs, null (default: null).",
      defaultValue = "null",
      names = {"-p", "--profiler"},
      paramLabel = "<profiler>")
  private Profiler profiler;

  @Option(
      description = "The application's classpath.",
      defaultValue = "",
      names = {"--app-cp"})
  private String classpath;

  @Option(
      names = {"-h", "--help"},
      description = "Display help and exit.",
      usageHelp = true)
  private boolean help;

  private static class ProfilerConverter implements ITypeConverter<Profiler> {
    @Override
    public Profiler convert(final String value) throws Exception {

      final Profiler profiler = Profiler.fromString(value);
      if (profiler == null)
        throw new TypeConversionException("invalid profiler provided: " + value);

      return profiler;
    }
  }

  @Override
  public Integer call() throws Exception {
    final MoiraFactory factory = parent.factory();
    final Service service = factory.createService();

    if (!classpath.isEmpty()) service.setAppClassPath(classpath);
    final Map<TestCase, Set<TestCase>> result = service.profile(profiler, testsuite);

    result.entrySet().stream()
        .forEach(
            entry ->
                entry.getValue().stream()
                    .forEach(
                        value ->
                            spec.commandLine()
                                .getOut()
                                .printf("from: %s, to: %s\n", entry.getKey(), value)));

    return 0;
  }
}
