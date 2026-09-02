package com.example;

import junit.framework.TestCase;

public class JUnit3FirstChildSimpleTest extends TestCase {

  public void testFailing() {
    assertTrue(false);
  }

  public void testPassing() {
    assertTrue(true);
  }
}
