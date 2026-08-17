package com.example;

import junit.framework.TestCase;

public class JUnit3ParametrizedTest extends TestCase {

  public JUnit3ParametrizedTest(final int parameter) {
    super("testSomething");
  }

  public void testSomething() {}
}
