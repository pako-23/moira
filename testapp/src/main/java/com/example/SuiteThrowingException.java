package com.example;

import junit.framework.Test;
import junit.framework.TestCase;

public class SuiteThrowingException extends TestCase {
  public static Test suite() {
      throw new RuntimeException("some errror");
  }
}
