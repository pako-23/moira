package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppObjectFieldTest {
  private static final AppObjectField object = new AppObjectField();

  @Test
  public void testReadFieldX() {
    assertEquals(0, object.x);
  }

  @Test
  public void testWriteFieldX() {
    object.x = 1;
    assertEquals(1, object.x);
  }

  @Test
  public void testReadFieldY() {
    assertEquals(0, object.y);
  }

  @Test
  public void testWriteFieldY() {
    object.y = 1;
    assertEquals(1, object.y);
  }
}
