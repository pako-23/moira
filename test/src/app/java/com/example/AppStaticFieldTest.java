package com.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppStaticFieldTest {
  @Test
  public void testReadFieldX() {
    assertEquals(0, AppStaticField.x);
  }

  @Test
  public void testWriteFieldX() {
    AppStaticField.x = 1;
    assertEquals(1, AppStaticField.x);
  }

  @Test
  public void testReadFieldY() {
    assertEquals(0, AppStaticField.y);
  }

  @Test
  public void testWriteFieldY() {
    AppStaticField.y = 1;
    assertEquals(1, AppStaticField.y);
  }
}
