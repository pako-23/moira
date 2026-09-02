package com.example;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3SuiteTestAll extends TestCase {

  public static Test suite() {
    final TestSuite suite = new TestSuite();

    suite.addTest(JUnit3SuiteFirstChildTest.suite());
    suite.addTest(JUnit3SuiteSecondChildTest.suite());

    return suite;
  }
}
