package com.example;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3SuiteFirstChildTest extends TestCase {
  public static Test suite() {
    return new TestSuite(JUnit3FirstChildSimpleTest.class);
  }
}
