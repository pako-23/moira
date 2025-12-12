package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppArrayTest {
  private static final int[] array = new int[10];

  @Test
  public void testWriteFirstIndex() {
    array[0] = 1;
    assertEquals(1, array[0]);
  }

  @Test
  public void testWriteSecondIndex() {
    array[1] = 1;
    assertEquals(1, array[1]);
  }

  @Test
  public void testReadFirstIndex() {
    assertEquals(1, array[0]);
  }

  @Test
  public void testReadSecondIndex() {
    assertEquals(1, array[1]);
  }
}
