package moira.util.docker;

import java.nio.charset.StandardCharsets;

public abstract class LineContainerStream implements ContainerStream {
  private static final int INITIAL_CAPACITY = 128;

  private final StringBuffer buffer;

  public LineContainerStream() {
    this.buffer = new StringBuffer(INITIAL_CAPACITY);
  }

  public void append(final byte[] data) {
    buffer.append(new String(data, StandardCharsets.UTF_8));

    int start = 0;
    for (int i = 0; i < buffer.length(); ++i) {
      if (buffer.charAt(i) != '\n') continue;

      processLine(buffer.subSequence(start, i));
      start = i + 1;
    }

    buffer.delete(0, start);
  }

  protected abstract void processLine(final CharSequence line);
}
