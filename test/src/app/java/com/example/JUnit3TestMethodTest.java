package com.example;

import junit.framework.TestCase;

public class JUnit3TestMethodTest extends TestCase {

  public void testSomething() {}

  private void testInvalidMethod() {}

  public void someOtherMethod() {
    testInvalidMethod();
  }
}
