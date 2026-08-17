package moira.util.execution;

public interface Executor {
  public Execution execution();

  public void setClassPath(final String classpath);
}
