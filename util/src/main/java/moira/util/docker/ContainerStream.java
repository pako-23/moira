package moira.util.docker;

public interface ContainerStream {
  public void append(final byte[] data);
}
