package com.example;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;



public class DescribableTestSuite extends TestCase {

  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new DescribableTestCase("testPassing"));
    suite.addTest(new JUnit3CustomTest());
    return suite;
  }
}
