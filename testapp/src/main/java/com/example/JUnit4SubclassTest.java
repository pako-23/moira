package com.example;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JUnit4SubclassTest extends JUnit4AbstractTest {
  @Override
  public void testAbstractMethod() {}

  @Test
  public void testSubclassPassingTest() {
    assertTrue(true);
  }

  @Test
  public void testSubclassFailingTest() {
    assertTrue(false);
  }
}
