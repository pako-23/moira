package ch.usi.inf.profiler;

import ch.usi.inf.collect.Map;
import ch.usi.inf.collect.MapBuilder;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ProfilerDump {
  private int capacity;
  private int size;
  private TestCase[] tests;

  private static class TestCase {
    public final String name;
    public Map<Integer, Void> dependencies;

    public TestCase(final String name) {
      this.name = name;
      dependencies = null;
    }

    public void registerDependency(final int target) {
      if (dependencies == null) {
        dependencies = MapBuilder.<Integer, Void>builder().build();
      }

      dependencies.getOrPut(target, () -> null);
    }
  }

  public ProfilerDump() {
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

  public void registerDependency(int dependant, int dependee) {
    this.tests[dependant].registerDependency(dependee);
  }

  public String getTestName(int i) {
    return tests[i].name;
  }

  public void dump(final String fileName) throws FileNotFoundException {
    try (PrintWriter writer = new PrintWriter(fileName)) {
      final Iterator it = iterator();

      while (it.hasNext()) {
        int dependant = it.getDependant();
        int dependee = it.getDependee();

        writer.println(getTestName(dependant) + " " + getTestName(dependee));
        it.next();
      }
    }
  }

  public void computeConflicts(final ReadWriteSet set) {
    for (int i = 0; i < set.size(); ++i) {
      if ((set.getMask(i) & ReadWriteSet.WRITE) == 0) continue;

      for (int j = 0; j < set.size(); ++j) {
        if (i == j || (set.getMask(j) & ReadWriteSet.READ_BEFORE_WRITE) == 0) continue;

        registerDependency(set.getTest(j), set.getTest(i));
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
      while (index < size && tests[index].dependencies == null) ++index;

      if (index < size) {
        iterator = tests[index].dependencies.iterator();
      }
    }

    public int getDependee() {
      return iterator.key();
    }

    public int getDependant() {
      return index;
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
