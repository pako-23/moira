package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

public class MoiraTest {
  @Test
  public void testSimpleMoiraExecution() throws IOException, InterruptedException {
    final Process process = TestUtils.moiraDefaultsCommand();
    process.waitFor();
    assertThat(TestUtils.readOutputStream(process.getErrorStream()).length(), is(0));
  }

  @Test
  public void testSinglePassingTest() throws IOException, InterruptedException {
    final Process process = TestUtils.moiraDefaultsCommand("com.example.SimplePassingTest");
    process.waitFor();
    assertThat(TestUtils.readOutputStream(process.getErrorStream()).length(), is(0));
  }

  @Test
  public void testMultiplePassingTest() throws IOException, InterruptedException {
    final Process process =
        TestUtils.moiraDefaultsCommand(
            "com.example.SimplePassingTest", "com.example.OtherPassingTest");
    process.waitFor();
    assertThat(TestUtils.readOutputStream(process.getErrorStream()).length(), is(0));
  }

  @Test
  public void testSingleFailingTest() throws IOException, InterruptedException {
    final Process process = TestUtils.moiraDefaultsCommand("com.example.SimpleFailingTest");
    process.waitFor();
  }

  @Test
  public void testMultipleTestsSingleFailing() throws IOException, InterruptedException {
    final Process process =
        TestUtils.moiraDefaultsCommand(
            "com.example.SimplePassingTest",
            "com.example.SimpleFailingTest",
            "com.example.OtherPassingTest");
    process.waitFor();
    final String output = TestUtils.readOutputStream(process.getInputStream());
    assertThat(output, containsString("com.example.SimpleFailingTest"));
  }

  @Test
  public void testNotExistingTest() throws IOException, InterruptedException {
    final Process process = TestUtils.moiraDefaultsCommand("com.example.NotExisting");
    process.waitFor();
    assertThat(
        TestUtils.readOutputStream(process.getErrorStream()),
        containsString("Could not find class[com.example.NotExisting]"));
  }
}
