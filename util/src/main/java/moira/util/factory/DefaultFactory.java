package moira.util.factory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import moira.util.FlakyPairsCollector;
import moira.util.PairsCollector;
import moira.util.TuscanSquareCollector;
import moira.util.execution.ForkExecutor;
import moira.util.model.TestCase;
import moira.util.runner.ScheduleGenerator;
import moira.util.service.DefaultService;
import moira.util.service.Service;
import moira.util.tuscan.PairCover;
import moira.util.tuscan.TargetPairsGenerator;
import moira.util.tuscan.TuscanClassOnly;
import moira.util.tuscan.TuscanInterClass;
import moira.util.tuscan.TuscanIntraClass;
import moira.util.tuscan.TuscanPacked;

public class DefaultFactory implements MoiraFactory {
  @Override
  public Service createService() {
    return new DefaultService(new ForkExecutor());
  }

  @Override
  public ScheduleGenerator createScheduleGenerator(
      final DetectionMode mode, final Service service, final File source) {
    switch (mode) {
      case TUSCAN_CLASS_ONLY:
        return new TuscanClassOnly(service.discoverTestSuite(source));
      case TUSCAN_INTRA_CLASS:
        return new TuscanIntraClass(service.discoverTestSuite(source));
      case TUSCAN_INTER_CLASS:
        return new TuscanInterClass(service.discoverTestSuite(source));
      case TARGET_PAIRS:
        return new TargetPairsGenerator(parsePairs(source));
      case MOIRA:
        return new PairCover(parsePairs(source));
      default:
        return new TuscanPacked(service.discoverTestSuite(source));
    }
  }

  @Override
  public FlakyPairsCollector createFlakyPairsCollector(
      final DetectionMode mode, final File source) {

    switch (mode) {
      case TUSCAN_CLASS_ONLY:
      case TUSCAN_INTER_CLASS:
      case TUSCAN_INTRA_CLASS:
      case TUSCAN_PACKED:
        return new TuscanSquareCollector();
      default:
        return new PairsCollector(parsePairs(source));
    }
  }

  private static Map<TestCase, Set<TestCase>> parsePairs(final File source) {
    final Map<TestCase, Set<TestCase>> pairs = new HashMap<>();
    final Map<String, TestCase> cases = parseTestCases(source);

    iterateTestCases(
        source,
        (from, to) -> {
          pairs.computeIfAbsent(cases.get(from), key -> new HashSet<>()).add(cases.get(to));
        });

    return pairs;
  }

  private static Map<String, TestCase> parseTestCases(final File source) {
    final Map<String, TestCase> cases = new HashMap<>();

    iterateTestCases(
        source,
        (from, to) -> {
          cases.putIfAbsent(from, TestCase.fromId(from));
          cases.putIfAbsent(to, TestCase.fromId(to));
        });

    return cases;
  }

  private static void iterateTestCases(
      final File source, final BiConsumer<String, String> consumer) {
    try (final Scanner scanner = new Scanner(source)) {
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine().trim();
        if (line.isEmpty()) continue;

        final String[] parts = line.split(", to:");
        if (parts.length != 2) continue;

        final String from = parts[0].substring("from: ".length());
        final String to = parts[1].trim();

        consumer.accept(from, to);
      }
    } catch (final IOException e) {
      throw new RuntimeException("failed to read pairs file: " + e.getMessage());
    }
  }
}
