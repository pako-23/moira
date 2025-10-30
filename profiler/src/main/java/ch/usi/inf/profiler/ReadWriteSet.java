package ch.usi.inf.profiler;

public class ReadWriteSet {
  public static byte READ = 0x1;
  public static byte WRITE = 0x2;
  public static byte READ_BEFORE_WRITE = 0x4;

  private int base;
  private int capacity;
  private int length;
  private byte items[];

  public ReadWriteSet(int base) {
    this.base = base;
    this.capacity = 16;
    this.length = 0;
    this.items = new byte[this.capacity];
  }

  public int min() {
    return base;
  }

  public int max() {
    return base + length;
  }

  public byte get(int item) {
    return items[item - base];
  }

  public void update(int item, byte event) {
    int index = item - base;

    if (index >= capacity) {
      grow(index);
    }

    if (length <= index) {
      length = index + 1;
    }

    if (items[index] == 0 && event == READ) items[index] |= READ_BEFORE_WRITE;

    items[index] |= event;
  }

  private void grow(int min) {
    int newCapacity = this.capacity << 1;

    while (min >= newCapacity) newCapacity <<= 1;

    byte[] items = new byte[newCapacity];
    for (int i = 0; i < length; ++i) items[i] = this.items[i];

    this.items = items;
    this.capacity = newCapacity;
  }
}
