package moira.profiler;

public class ThreadSuspension {
  private static int INITIAL_CAPCITY = 32;

  private int[] suspensionVector;
  private Thread[] threads;
  private int size;

  public ThreadSuspension() {
    suspensionVector = new int[INITIAL_CAPCITY];
    threads = new Thread[INITIAL_CAPCITY];
    size = 0;
  }

  public void suspend() {
    suspend(Thread.currentThread());
  }

  public void resume() {
    resume(Thread.currentThread());
  }

  public boolean suspendedOrSuspend() {
    return suspendedOrSuspend(Thread.currentThread());
  }

  private synchronized boolean suspendedOrSuspend(final Thread thread) {
    for (int i = 0; i < size; ++i) {
      if (threads[i] != thread) continue;

      if (i > 0) {
        int count = suspensionVector[i];

        threads[i] = threads[0];
        suspensionVector[i] = suspensionVector[0];
        threads[0] = thread;
        suspensionVector[0] = count;
      }

      return true;
    }

    if (size > 0) {
      if (size == threads.length) grow();
      threads[size] = threads[0];
      suspensionVector[size] = suspensionVector[0];
    }

    size += 1;
    threads[0] = thread;
    suspensionVector[0] = 1;
    return false;
  }

  private synchronized void suspend(final Thread thread) {
    for (int i = 0; i < size; ++i) {
      if (threads[i] != thread) continue;

      ++suspensionVector[i];
      if (i > 0) {
        int count = suspensionVector[i];

        threads[i] = threads[0];
        suspensionVector[i] = suspensionVector[0];
        threads[0] = thread;
        suspensionVector[0] = count;
      }

      return;
    }

    if (size > 0) {
      if (size == threads.length) grow();
      threads[size] = threads[0];
      suspensionVector[size] = suspensionVector[0];
    }

    size += 1;
    threads[0] = thread;
    suspensionVector[0] = 1;
  }

  private synchronized void resume(final Thread thread) {
    for (int i = 0; i < size; ++i) {
      if (threads[i] != thread) continue;
      if (--suspensionVector[i] > 0) return;

      if (i + 1 < size) {
        int j = size - 1;

        suspensionVector[i] = suspensionVector[j];
        threads[i] = threads[j];
        threads[j] = null;
      } else {
        threads[i] = null;
      }
      --size;
      break;
    }
  }

  private void grow() {
    int capacity = threads.length << 1;
    int[] suspensionVector = new int[capacity];
    Thread[] threads = new Thread[capacity];
    for (int i = 0; i < size; ++i) {
      suspensionVector[i] = this.suspensionVector[i];
      threads[i] = this.threads[i];
    }
    this.suspensionVector = suspensionVector;
    this.threads = threads;
  }
}
