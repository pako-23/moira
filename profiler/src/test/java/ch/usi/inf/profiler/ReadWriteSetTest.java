package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ReadWriteSetTest {

  @Test
  public void createSet() {
    ReadWriteSet set = new ReadWriteSet(0);

    assertEquals(0, set.min());
    assertEquals(0, set.max());
  }

  @Test
  public void createSetBase() {
    ReadWriteSet set = new ReadWriteSet(10);

    assertEquals(10, set.min());
    assertEquals(10, set.max());
  }

  @Test
  public void readUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    singleEventActions(10, updated, ReadWriteSet.READ);
  }

  @Test
  public void writeUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    singleEventActions(10, updated, ReadWriteSet.WRITE);
  }

  @Test
  public void readBeforeWriteUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    ReadWriteSet set = new ReadWriteSet(7);

    for (int i = 0; i < updated.length; ++i) {
      if (i % 2 == 0) {
        set.update(updated[i], ReadWriteSet.READ);
      } else {
        set.update(updated[i], ReadWriteSet.WRITE);
      }
    }

    assertEquals(7, set.min());
    assertEquals(updated[updated.length - 1] + 1, set.max());

    for (int it = set.min(), i = 0; it < set.max(); ++it) {
      if (it != updated[i]) {
        assertEquals(0, set.get(it));
      } else if (i % 2 == 0) {
        assertEquals(ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ, set.get(it));
        ++i;
      } else {
        assertEquals(ReadWriteSet.WRITE, set.get(it));
        ++i;
      }
    }
  }

  @Test
  public void multipleEvents() {
    int base = 3;
    ReadWriteSet set = new ReadWriteSet(base);

    for (int i = 0; i < 5; ++i) {
      set.update(base + i, ReadWriteSet.READ);
      set.update(base + i, ReadWriteSet.WRITE);
    }

    assertEquals(base, set.min());
    assertEquals(base + 5, set.max());

    int expected = ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ | ReadWriteSet.WRITE;
    for (int i = 0; i < 5; ++i) {
      assertEquals(expected, set.get(base + i));
    }
  }

  @Test
  public void resize() {
    int[] updated = new int[] {0, 8, 16, 20, 33};
    singleEventActions(0, updated, ReadWriteSet.READ);
  }

  @Test
  public void resizeLongSkip() {
    int[] updated = new int[] {0, 8, 16, 128};
    singleEventActions(0, updated, ReadWriteSet.READ);
  }

  private void singleEventActions(int base, int[] updated, byte event) {
    ReadWriteSet set = new ReadWriteSet(base);

    for (int item : updated) {
      set.update(item, event);
    }

    assertEquals(updated[0], set.min());
    assertEquals(updated[updated.length - 1] + 1, set.max());

    for (int it = set.min(), i = 0; it < set.max(); ++it) {
      if (it != updated[i]) {
        assertEquals(0, set.get(it));
        continue;
      } else if (event == ReadWriteSet.READ) {
        assertEquals(ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ, set.get(it));
      } else {
        assertEquals(event, set.get(it));
      }

      ++i;
    }
  }
}
