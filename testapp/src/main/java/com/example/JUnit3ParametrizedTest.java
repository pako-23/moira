package com.example;

import junit.framework.TestCase;

public class JUnit3ParametrizedTest extends TestCase {

  private final int parameter;

  public JUnit3ParametrizedTest(final int parameter) {
    super("testParameter");
    this.parameter = parameter;
  }

  public void testParameter() {

    assertEquals(10, parameter);
  }
}
