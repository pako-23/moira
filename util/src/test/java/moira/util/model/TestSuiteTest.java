package moira.util.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class TestSuiteTest {
  @Test
  public void testEmptyCases() throws IOException {
    final TestSuite suite = new TestSuite(new ArrayList<>());
    assertThat(suite.numberOfTestCases(), is(0));
    assertThat(suite.numberOfTestClasses(), is(0));
  }

  @Test
  public void testSingleTestCase() throws IOException {
    final TestCase tc = new TestCase("com.Foo[test1]");
    final TestSuite suite = new TestSuite(Arrays.asList(tc));

    assertThat(suite.numberOfTestCases(), is(1));
    assertThat(suite.numberOfTestClasses(), is(1));
    assertThat(suite.getTestCase(0), is(tc));
    assertThat(suite.getTestClass(0), is("com.Foo"));

    final Range range = suite.getTestClassCases("com.Foo");
    assertThat(range.min(), is(0));
    assertThat(range.max(), is(1));
  }

  @Test
  public void testSingleClassMultipleCases() throws IOException {
    final TestCase tc1 = new TestCase("com.Foo[test1]");
    final TestCase tc2 = new TestCase("com.Foo[test2]");
    final TestCase tc3 = new TestCase("com.Foo[test3]");
    final TestSuite suite = new TestSuite(Arrays.asList(tc1, tc2, tc3));

    assertThat(suite.numberOfTestCases(), is(3));
    assertThat(suite.numberOfTestClasses(), is(1));
    assertThat(suite.getTestCase(0), is(tc1));
    assertThat(suite.getTestCase(1), is(tc2));
    assertThat(suite.getTestCase(2), is(tc3));
    assertThat(suite.getTestClass(0), is("com.Foo"));

    final Range range = suite.getTestClassCases("com.Foo");
    assertThat(range.min(), is(0));
    assertThat(range.max(), is(3));
  }

  @Test
  public void testMultipleClasses() throws IOException {
    final TestCase tc1 = new TestCase("com.Foo[test1]");
    final TestCase tc2 = new TestCase("com.Foo[test2]");
    final TestCase tc3 = new TestCase("com.Bar[test1]");
    final TestCase tc4 = new TestCase("com.Baz[test1]");
    final TestSuite suite = new TestSuite(Arrays.asList(tc1, tc2, tc3, tc4));

    assertThat(suite.numberOfTestCases(), is(4));
    assertThat(suite.numberOfTestClasses(), is(3));
    assertThat(suite.getTestCase(0), is(tc1));
    assertThat(suite.getTestCase(1), is(tc2));
    assertThat(suite.getTestCase(2), is(tc3));
    assertThat(suite.getTestCase(3), is(tc4));
    assertThat(suite.getTestClass(0), is("com.Foo"));
    assertThat(suite.getTestClass(1), is("com.Bar"));
    assertThat(suite.getTestClass(2), is("com.Baz"));

    assertThat(suite.getTestClassCases("com.Foo").min(), is(0));
    assertThat(suite.getTestClassCases("com.Foo").max(), is(2));
    assertThat(suite.getTestClassCases("com.Bar").min(), is(2));
    assertThat(suite.getTestClassCases("com.Bar").max(), is(3));
    assertThat(suite.getTestClassCases("com.Baz").min(), is(3));
    assertThat(suite.getTestClassCases("com.Baz").max(), is(4));
  }

  @Test
  public void testInterleavedSameClassNotGrouped() throws IOException {
    final TestCase tc1 = new TestCase("com.Foo[test1]");
    final TestCase tc2 = new TestCase("com.Bar[test1]");
    final TestCase tc3 = new TestCase("com.Foo[test2]");
    final TestSuite suite = new TestSuite(Arrays.asList(tc1, tc2, tc3));

    assertThat(suite.numberOfTestClasses(), is(3));
    assertThat(suite.numberOfTestCases(), is(3));
    assertThat(suite.getTestClass(0), is("com.Foo"));
    assertThat(suite.getTestClass(1), is("com.Bar"));
    assertThat(suite.getTestClass(2), is("com.Foo"));

    assertThat(suite.getTestClassCases("com.Foo").min(), is(2));
    assertThat(suite.getTestClassCases("com.Foo").max(), is(3));
    assertThat(suite.getTestClassCases("com.Bar").min(), is(1));
    assertThat(suite.getTestClassCases("com.Bar").max(), is(2));
  }

  @Test
  public void testGetTestClassCasesReturnsNullForUnknownClass() throws IOException {
    final TestCase tc = new TestCase("com.Foo[test1]");
    final TestSuite suite = new TestSuite(Arrays.asList(tc));
    assertThat(suite.getTestClassCases("com.Unknown"), is((Range) null));
  }
}
