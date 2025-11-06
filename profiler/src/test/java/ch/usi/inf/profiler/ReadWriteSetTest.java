package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ReadWriteSetTest {

  @Test
  public void createSet() {
    ReadWriteSet set = new ReadWriteSet();

    assertEquals(0, set.size());
  }

  @Test
  public void createSetBase() {
    ReadWriteSet set = new ReadWriteSet();

    set.update(10, ReadWriteSet.WRITE);
    assertEquals(1, set.size());
    assertEquals(10, set.getTest(0));
    assertEquals(ReadWriteSet.WRITE, set.getMask(0));
  }

  @Test
  public void readUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    singleEventActions(updated, ReadWriteSet.READ);
  }

  @Test
  public void writeUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    singleEventActions(updated, ReadWriteSet.WRITE);
  }

  @Test
  public void readBeforeWriteUpdate() {
    int[] updated = new int[] {10, 11, 13, 18};
    ReadWriteSet set = new ReadWriteSet();

    for (int i = 0; i < updated.length; ++i) {
      if (i % 2 == 0) {
        set.update(updated[i], ReadWriteSet.READ);
      } else {
        set.update(updated[i], ReadWriteSet.WRITE);
      }
    }

    assertEquals(updated.length, set.size());

    for (int i = 0; i < set.size(); ++i) {
      assertEquals(updated[i], set.getTest(i));
      if (i % 2 == 0) {
        assertEquals(ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ, set.getMask(i));
      } else {
        assertEquals(ReadWriteSet.WRITE, set.getMask(i));
      }
    }
  }

  @Test
  public void multipleEvents() {
    int base = 3;
    ReadWriteSet set = new ReadWriteSet();

    for (int i = 0; i < 5; ++i) {
      set.update(base + i, ReadWriteSet.READ);
      set.update(base + i, ReadWriteSet.WRITE);
    }

    assertEquals(5, set.size());

    int expected = ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ | ReadWriteSet.WRITE;
    for (int i = 0; i < 5; ++i) {
      assertEquals(base + i, set.getTest(i));
      assertEquals(expected, set.getMask(i));
    }
  }

  @Test
  public void resize() {
    int[] updated = new int[40];

    for (int i = 0; i < updated.length; ++i) updated[i] = 10 + i;

    singleEventActions(updated, ReadWriteSet.READ);
  }

  @Test
  public void multipleResize() {
    int[] updated = new int[140];

    for (int i = 0; i < updated.length; ++i) updated[i] = 2 * i;

    singleEventActions(updated, ReadWriteSet.READ);
  }

  private void singleEventActions(int[] updated, byte event) {
    ReadWriteSet set = new ReadWriteSet();

    for (int item : updated) {
      set.update(item, event);
    }

    assertEquals(updated.length, set.size());

    for (int i = 0; i < set.size(); ++i) {
      assertEquals(updated[i], set.getTest(i));
      if (event == ReadWriteSet.READ) {
        assertEquals(ReadWriteSet.READ_BEFORE_WRITE | ReadWriteSet.READ, set.getMask(i));
      } else {
        assertEquals(event, set.getMask(i));
      }
    }
  }
}
