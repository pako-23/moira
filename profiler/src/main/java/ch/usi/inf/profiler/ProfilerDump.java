package ch.usi.inf.profiler;

public class ProfilerDump {
  private int capacity;
  private int size;
  private String[] testNames;
  private byte[][] dependencies;
  private volatile int runningTest;

  public ProfilerDump() {
    capacity = 128;
    size = 0;
    testNames = new String[capacity];
    dependencies = new byte[capacity][capacity];
    runningTest = -1;
  }

  public int getRunningTest() {
    return runningTest;
  }

  public void registerTest(final String test) {
    if (size == capacity) {
      grow();
    }

    this.runningTest = size;
    testNames[size++] = test;
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

  public void unregisterTest() {
    runningTest = -1;
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
