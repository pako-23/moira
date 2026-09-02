package com.example;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3SuiteSecondChildTest extends TestCase {
  public static Test suite() {
    final TestSuite suite = new TestSuite();

    suite.addTest(new JUnit3ParametrizedTest(10));
    suite.addTest(new JUnit3ParametrizedTest(11));
    suite.addTest(new JUnit3ParametrizedTest(12));

    return suite;
  }
}
