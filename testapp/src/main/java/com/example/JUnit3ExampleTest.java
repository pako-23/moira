package com.example;

import junit.framework.TestCase;

public class JUnit3ExampleTest extends TestCase {
  public void testSimplePassing() {
    assertTrue(true);
  }

  public void testSimpleFailing() {
    assertTrue(false);
  }
}
