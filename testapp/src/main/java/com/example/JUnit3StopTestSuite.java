package com.example;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3StopTestSuite extends TestCase {
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new JUnit3StoppingTest());
    suite.addTest(new JUnit3CustomTest());
    return suite;
  }
}