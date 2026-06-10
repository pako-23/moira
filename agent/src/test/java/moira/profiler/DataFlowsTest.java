package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DataFlowsTest {
  private DataFlows dataFlows;

  @BeforeEach
  public void setup() {
    dataFlows = new DataFlows();
  }

  @Test
  public void testTestRegistration() {
    registerTest("TestA");
  }

  @Test
  public void testMultipleTestRegistration() {
    for (int i = 0; i < 500; ++i) registerTest("Test" + i);
  }

  @Test
  public void testNoDataFlows() {
    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testRegisterDependency() {
    registerTests("A", "B", "C");

    dataFlows.registerDataFlow(2, 0);

    checkContainsDataFlows(
        Stream.of(
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    2, new HashSet<>(Arrays.asList(0))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @Test
  public void testIteratorMultipleDataFlows() {
    registerTests("A", "B", "C");

    final Map<Integer, Set<Integer>> pairs =
        Stream.of(
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    0, new HashSet<>(Arrays.asList(2))),
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    1, new HashSet<>(Arrays.asList(0, 2))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    for (final Map.Entry<Integer, Set<Integer>> entry : pairs.entrySet())
      for (final Integer target : entry.getValue())
        dataFlows.registerDataFlow(entry.getKey(), target);

    checkContainsDataFlows(pairs);
  }

  @Test
  public void testMultipleDataFlowsManyTests() {
    int capacity = 200;

    for (int i = 0; i < capacity; ++i) dataFlows.registerTest("Test" + i);

    final Map<Integer, Set<Integer>> pairs = new HashMap<>();

    for (int i = 0; i < capacity; i += 2) {
      final Set<Integer> targets = new HashSet<>();

      for (int j = 0; j < i; j += 2) {
        dataFlows.registerDataFlow(i, j);
        targets.add(j);
      }

      if (!targets.isEmpty()) pairs.put(i, targets);
    }

    checkContainsDataFlows(pairs);
  }

  @Test
  public void testNoTestsDump() {
    assertThat(makeDump("no-tests-dump").size(), is(0));
  }

  @Test
  public void testExistingDependencies() {
    registerTests("TestA", "TestB", "TestC", "TestD");
    dataFlows.registerDataFlow(0, 1);
    dataFlows.registerDataFlow(2, 3);

    final List<String> lines = makeDump("some-dependencies-dump");

    assertThat(lines.size(), is(2));
    assertThat(lines, hasItems("from: TestA, to: TestB", "from: TestC, to: TestD"));
  }

  @Test
  public void testUpdateNoReadWriteEvents() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    dataFlows.update(new ReadWriteSet());

    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testUpdateWriteAfterWrite() {
    final ReadWriteSet set = new ReadWriteSet();

    registerTests("TestA", "TestB", "TestC", "TestD");

    set.update(1, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.WRITE);
    dataFlows.update(set);

    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testUpdateReadAfterRead() {
    final ReadWriteSet set = new ReadWriteSet();

    registerTests("TestA", "TestB", "TestC", "TestD");

    set.update(1, ReadWriteSet.READ);
    set.update(3, ReadWriteSet.READ);
    dataFlows.update(set);

    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testUpdateReadAfterWrite() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    final ReadWriteSet set = new ReadWriteSet();

    set.update(1, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.READ);
    dataFlows.update(set);

    checkContainsDataFlows(
        Stream.of(
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    1, new HashSet<>(Arrays.asList(3))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @Test
  public void testUpdateReadAfterWriteInverted() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    final ReadWriteSet set = new ReadWriteSet();

    set.update(1, ReadWriteSet.READ);
    set.update(3, ReadWriteSet.WRITE);
    dataFlows.update(set);

    checkContainsDataFlows(
        Stream.of(
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    3, new HashSet<>(Arrays.asList(1))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  @Test
  public void testUpdateReadAfterWriteRewritten() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    final ReadWriteSet set = new ReadWriteSet();

    set.update(1, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.READ);
    dataFlows.update(set);

    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testUpdateReadAfterWriteRewrittenBoth() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    final ReadWriteSet set = new ReadWriteSet();

    set.update(1, ReadWriteSet.WRITE);
    set.update(1, ReadWriteSet.READ);
    set.update(3, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.READ);
    dataFlows.update(set);

    checkContainsDataFlows(new HashMap<>());
  }

  @Test
  public void testComputeConflictsReadAfterReadRewritten() {
    registerTests("TestA", "TestB", "TestC", "TestD");

    final ReadWriteSet set = new ReadWriteSet();

    set.update(1, ReadWriteSet.READ);
    set.update(3, ReadWriteSet.WRITE);
    set.update(3, ReadWriteSet.READ);
    dataFlows.update(set);

    checkContainsDataFlows(
        Stream.of(
                new AbstractMap.SimpleEntry<Integer, Set<Integer>>(
                    3, new HashSet<>(Arrays.asList(1))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private void registerTest(final String test) {
    int runningTest = dataFlows.registerTest(test);
    assertThat(runningTest, greaterThanOrEqualTo(0));
    assertThat(test, is(dataFlows.getTestName(runningTest)));
  }

  private void checkContainsDataFlows(final Map<Integer, Set<Integer>> pairs) {
    DataFlows.Iterator iterator = dataFlows.iterator();

    while (iterator.hasNext()) {
      final Set<Integer> targets = pairs.get(iterator.getFrom());

      assertThat(targets, notNullValue());
      assertThat(targets.contains(iterator.getTo()), is(true));

      targets.remove(iterator.getTo());
      if (targets.isEmpty()) pairs.remove(iterator.getFrom());
      iterator.next();
    }

    assertThat(iterator.hasNext(), not(is(pairs.isEmpty())));
  }

  private void registerTests(final String... tests) {
    for (final String test : tests) registerTest(test);
  }

  private List<String> makeDump(final String fileName) {
    List<String> lines = null;

    try {
      File file = new File(fileName);
      file.deleteOnExit();

      dataFlows.dump(fileName);
      lines =
          Files.readAllLines(Paths.get(fileName)).stream().sorted().collect(Collectors.toList());
    } catch (final IOException e) {
      fail(e.getMessage());
    }

    return lines;
  }
}
