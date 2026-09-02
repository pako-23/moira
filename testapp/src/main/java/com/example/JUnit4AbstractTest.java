package com.example;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public abstract class JUnit4AbstractTest {
  @Test
  public void testPassing() {
    assertTrue(true);
  }

  @Test
  public void testFailing() {
    assertTrue(false);
  }

  @Test
  public abstract void testAbstractMethod();
}
