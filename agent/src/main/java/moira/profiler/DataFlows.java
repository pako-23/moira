package moira.profiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import moira.collect.Map;
import moira.collect.MapBuilder;

public class DataFlows {
  private int capacity;
  private int size;
  private TestCase[] tests;

  private static class TestCase {
    public final String name;
    public Map<Integer, Void> flows;

    public TestCase(final String name) {
      this.name = name;
      flows = null;
    }

    public void registerDataFlow(final int to) {
      if (flows == null) {
        flows = MapBuilder.<Integer, Void>builder().build();
      }

      flows.getOrPut(to, () -> null);
    }

    public boolean hasDataFlows() {
      return flows != null;
    }

    public Map.Iterator<Integer, Void> iterator() {
      return flows.iterator();
    }
  }

  public DataFlows() {
    capacity = 128;
    size = 0;
    tests = new TestCase[capacity];
  }

  public int registerTest(final String test) {
    if (size == capacity) {
      grow();
    }

    tests[size] = new TestCase(test);
    return size++;
  }

  private void grow() {
    int capacity = this.capacity;
    this.capacity <<= 1;
    final TestCase[] tests = new TestCase[this.capacity];

    for (int i = 0; i < capacity; ++i) tests[i] = this.tests[i];
    this.tests = tests;
  }

  public void registerDataFlow(final int from, final int to) {
    this.tests[from].registerDataFlow(to);
  }

  public String getTestName(final int i) {
    return tests[i].name;
  }

  public void dump(final String fileName) throws FileNotFoundException {
    try (PrintWriter writer = new PrintWriter(fileName)) {
      final Iterator it = iterator();

      while (it.hasNext()) {
        final int from = it.getFrom();
        final int to = it.getTo();

        writer.println("from: " + getTestName(from) + ", to: " + getTestName(to));
        it.next();
      }
    }
  }

  public void update(final ReadWriteSet set) {
    for (int i = 0; i < set.size(); ++i) {
      if (!set.isWriteEvent(i)) continue;

      for (int j = 0; j < set.size(); ++j) {
        if (i == j || !set.isReadBeforeWriteEvent(j)) continue;

        registerDataFlow(set.getTest(i), set.getTest(j));
      }
    }
  }

  public Iterator iterator() {
    return new Iterator();
  }

  public class Iterator {
    private int index;
    private Map.Iterator<Integer, Void> iterator;

    public Iterator() {
      index = 0;
      advance();
    }

    private void advance() {
      while (index < size && !tests[index].hasDataFlows()) ++index;

      if (index < size) {
        iterator = tests[index].iterator();
      }
    }

    public int getFrom() {
      return index;
    }

    public int getTo() {
      return iterator.key();
    }

    public boolean hasNext() {
      return index < size;
    }

    public void next() {
      iterator.next();
      if (iterator.hasNext()) return;

      ++index;
      advance();
    }
  }
}
