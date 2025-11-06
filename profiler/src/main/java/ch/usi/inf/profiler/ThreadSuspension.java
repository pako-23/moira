package ch.usi.inf.profiler;

public class ThreadSuspension {
  private ThreadLocal<Boolean> suspended;

  public ThreadSuspension() {
    suspended = new ThreadLocal<>();
  }

  public void suspend() {
    suspended.set(true);
  }

  public boolean isSuspended() {
    return suspended.get() != null && suspended.get();
  }

  public void resume() {
    suspended.set(false);
  }
}
