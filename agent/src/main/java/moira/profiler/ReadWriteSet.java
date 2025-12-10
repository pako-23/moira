package moira.profiler;

public class ReadWriteSet {
  public static byte READ = 0x1;
  public static byte WRITE = 0x2;
  public static byte READ_BEFORE_WRITE = 0x4;

  private static int INITIAL_CAPACITY = 4;

  private int size;
  private byte masks[];
  private int tests[];

  public ReadWriteSet() {
    this.size = 0;
    this.masks = new byte[INITIAL_CAPACITY];
    this.tests = new int[INITIAL_CAPACITY];
  }

  public int size() {
    return size;
  }

  public byte getMask(int i) {
    return masks[i];
  }

  public int getTest(int i) {
    return tests[i];
  }

  public void update(int test, byte event) {
    int i;

    if (size == 0) {
      i = 0;
      tests[i] = test;
      ++size;
    } else if (tests[size - 1] == test) {
      i = size - 1;
    } else {
      i = size;
      if (i == masks.length) grow();
      tests[i] = test;
      ++size;
    }

    if (masks[i] == 0 && event == READ) masks[i] |= READ_BEFORE_WRITE;
    masks[i] |= event;
  }

  private void grow() {
    int capacity = masks.length << 1;
    byte[] masks = new byte[capacity];
    int[] tests = new int[capacity];

    for (int i = 0; i < size; ++i) {
      masks[i] = this.masks[i];
      tests[i] = this.tests[i];
    }

    this.masks = masks;
    this.tests = tests;
  }
}
