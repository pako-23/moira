package com.example;

import junit.framework.Test;
import junit.framework.TestCase;

public class SingleTestCaseSuiteMethod extends TestCase {

    public SingleTestCaseSuiteMethod(final String test) {
        super(test);
    }
    
  public static Test suite() {
    return new SingleTestCaseSuiteMethod("testPassing");
  }

      public void testPassing() {
    assertTrue(true);
  }

}
