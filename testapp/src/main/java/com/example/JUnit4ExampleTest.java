package com.example;


import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

public class JUnit4ExampleTest {

  @Test
  public void testSimplePassing() {
    assertTrue(true);
  }

  @Test
  public void testSimpleFailing() {
    assertTrue(false);
  }

    @Ignore("not running this test")
    @Test
    public void testIgnored() {
        assertTrue(false);
    }
}
