package com.example;

import static org.junit.Assert.fail;

import org.junit.Test;

public class SimpleFailingTest {
  @Test
  public void testFail() {
    fail("some random failure");
  }
}
