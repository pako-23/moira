package ch.usi.inf.profiler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ProfilerDump {
  private int capacity;
  private int size;
  private String[] testNames;
  private byte[][] dependencies;

  public ProfilerDump() {
    capacity = 128;
    size = 0;
    testNames = new String[capacity];
    dependencies = new byte[capacity][capacity];
  }

  public int registerTest(final String test) {
    if (size == capacity) {
      grow();
    }

    testNames[size] = test;
    return size++;
  }

  private void grow() {
    int capacity = this.capacity;
    this.capacity <<= 1;
    String[] testNames = new String[this.capacity];
    byte[][] dependencies = new byte[this.capacity][this.capacity];

    for (int i = 0; i < capacity; ++i) testNames[i] = this.testNames[i];
    this.testNames = testNames;

    for (int i = 0; i < capacity; ++i) {
      for (int j = 0; j < capacity; ++j) {
        dependencies[i][j] = this.dependencies[i][j];
      }
    }
    this.dependencies = dependencies;
  }

  public void registerDependency(int dependant, int dependee) {
    this.dependencies[dependant][dependee] = 1;
  }

  public String getTestName(int i) {
    return testNames[i];
  }

  public void dump(final String fileName) throws FileNotFoundException {
    try (PrintWriter writer = new PrintWriter(fileName)) {
      Iterator it = iterator();

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
    private int dependant;
    private int dependee;

    public Iterator() {
      dependee = 0;
      dependant = 0;
      advance();
    }

    private void advance() {
      while (hasNext() && dependencies[dependant][dependee] == 0) {
        increment();
      }
    }

    private void increment() {
      if (++dependee == capacity) {
        dependee = 0;
        ++dependant;
      }
    }

    public int getDependee() {
      return dependee;
    }

    public int getDependant() {
      return dependant;
    }

    public boolean hasNext() {
      return dependant < capacity;
    }

    public void next() {
      increment();
      advance();
    }
  }
}
