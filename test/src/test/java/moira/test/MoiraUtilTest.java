package moira.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MoiraUtilTest {
  @Test
  public void testHelpPage() throws IOException, InterruptedException {
    final Process noOptionsCommand = TestUtils.moiraUtilCommand();
    final Process helpCommnd = TestUtils.moiraUtilCommand("-h");

    noOptionsCommand.waitFor();
    helpCommnd.waitFor();

    final String noOptionsOutput = TestUtils.readOutputStream(noOptionsCommand.getInputStream());
    final String helpOutput = TestUtils.readOutputStream(helpCommnd.getInputStream());

    assertThat(noOptionsOutput.length(), not(is(0)));
    assertThat(helpOutput.length(), not(is(0)));
    assertThat(noOptionsOutput, is(helpOutput));
  }

  @Test
  public void testHelpCommand() throws IOException, InterruptedException {
    final Process noOptionsCommand = TestUtils.moiraUtilCommand("help", "-h");
    final Process helpCommand = TestUtils.moiraUtilCommand("help");

    noOptionsCommand.waitFor();
    helpCommand.waitFor();

    final String noOptionsOutput = TestUtils.readOutputStream(noOptionsCommand.getInputStream());
    final String helpOutput = TestUtils.readOutputStream(helpCommand.getErrorStream());

    assertThat(noOptionsOutput.length(), not(is(0)));
    assertThat(helpOutput.length(), not(is(0)));
    assertThat(helpOutput, containsString(noOptionsOutput));
  }

  @Test
  public void testHelpValidCommand() throws IOException, InterruptedException {
    final Process helpVerifyCommand = TestUtils.moiraUtilCommand("help", "verify");
    final Process verifyHelpOptionCommnd = TestUtils.moiraUtilCommand("verify", "-h");

    helpVerifyCommand.waitFor();
    verifyHelpOptionCommnd.waitFor();

    final String helpVerifyOutput = TestUtils.readOutputStream(helpVerifyCommand.getInputStream());
    final String verifyHelpOptionOutput =
        TestUtils.readOutputStream(verifyHelpOptionCommnd.getInputStream());

    assertThat(helpVerifyOutput.length(), not(is(0)));
    assertThat(verifyHelpOptionOutput.length(), not(is(0)));
    assertThat(helpVerifyOutput, is(verifyHelpOptionOutput));
  }

  @Test
  public void testHelpInvalidCommand() throws IOException, InterruptedException {
    final Process command = TestUtils.moiraUtilCommand("help", "somecommand");

    command.waitFor();

    final String output = TestUtils.readOutputStream(command.getInputStream());

    assertThat(output.length(), not(is(0)));
    assertThat(output, containsString("Unknown command: 'somecommand'"));
  }

  @Test
  public void testVersionCommand() throws IOException, InterruptedException {
    final Class<?> clazz = moira.util.cli.MoiraUtil.class;

    assertThat(clazz.isAnnotationPresent(picocli.CommandLine.Command.class), is(true));
    picocli.CommandLine.Command annotation = clazz.getAnnotation(picocli.CommandLine.Command.class);
    assertThat(annotation.version().length, is(1));
    final String version = annotation.version()[0];
    final Process command = TestUtils.moiraUtilCommand("-version");

    command.waitFor();

    final String output = TestUtils.readOutputStream(command.getInputStream());

    assertThat(output.length(), not(is(0)));
    assertThat(output, containsString(version));
  }

  @Test
  public void testVerifyDependencyFail() throws IOException, InterruptedException {
    final Process command =
        TestUtils.moiraUtilCommand(
            "verify",
            "com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)]",
            "com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]");

    command.waitFor();

    final List<String> expectedLines =
        Arrays.asList(
            "Running schedule:",
            "  com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)] -> PASS",
            "  com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)] -> FAIL");
    final String output = TestUtils.readOutputStream(command.getInputStream());

    assertThat(output.length(), not(is(0)));
    assertThat(output, containsString("NOT OK"));
    assertThat(output, containsString(String.join("\n", expectedLines)));
  }

  @Test
  public void testVerifyDependencyPass() throws IOException, InterruptedException {
    final Process command =
        TestUtils.moiraUtilCommand(
            "verify",
            "com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)]",
            "com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)]");

    command.waitFor();

    final List<String> expectedLines =
        Arrays.asList(
            "Running schedule:",
            "  com.example.AppObjectFieldTest[testReadFieldX(com.example.AppObjectFieldTest)] -> PASS",
            "  com.example.AppObjectFieldTest[testWriteFieldX(com.example.AppObjectFieldTest)] -> PASS");
    final String output = TestUtils.readOutputStream(command.getInputStream());

    assertThat(output.length(), not(is(0)));
    assertThat(output, not(containsString("NOT OK")));
    assertThat(output, containsString(String.join("\n", expectedLines)));
  }

  @Test
  public void testVerifyInvalidTestNames() throws IOException, InterruptedException {
    final Process command =
        TestUtils.moiraUtilCommand(
            "verify", "com.example.AppObjectFieldTest[]", "com.example.AppObjectFieldTest[]");

    command.waitFor();

    final String output = TestUtils.readOutputStream(command.getErrorStream());

    assertThat(output.length(), not(is(0)));
    assertThat(output, containsString("missing description from test: "));
  }
}
