package com.example;

import junit.framework.TestCase;
import org.junit.runner.Describable;
import org.junit.runner.Description;


public class DescribableTestCase extends TestCase implements Describable {

  public DescribableTestCase(final String test) {
    super(test);
  }

  public void testPassing() {
    assertTrue(true);
  }

  @Override
  public Description getDescription() {
    return Description.createTestDescription(DescribableTestCase.class, "customDescription");
  }
}
