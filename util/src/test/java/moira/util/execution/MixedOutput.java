package moira.util.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class MixedOutput {
  public static void main(final String... args) throws IOException {

    final PrintStream[] streams = new PrintStream[] {System.out, System.err};

    try (final BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
      String line;
      int stream = 0;

      while ((line = input.readLine()) != null) {
        streams[stream].println(line);
        stream = (stream + 1) % streams.length;
      }
    }
  }
}
