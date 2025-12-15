package moira.profiler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ThreadSuspensionTest {
  private ThreadSuspension suspension;

  @BeforeEach
  public void setUp() {
    suspension = new ThreadSuspension();
  }

  @Test
  public void testSimpleSuspendedOrSuspendAndResume() {
    assertThat(suspension.suspendedOrSuspend(), is(false));
    suspension.resume();
  }

  @Test
  public void testSimpleSuspendAndResume() {
    suspension.suspend();
    assertThat(suspension.suspendedOrSuspend(), is(true));
    suspension.resume();
  }

  @Test
  public void testMultipleSuspendedOrSuspend() {
    assertThat(suspension.suspendedOrSuspend(), is(false));
    assertThat(suspension.suspendedOrSuspend(), is(true));
    assertThat(suspension.suspendedOrSuspend(), is(true));

    suspension.resume();
    assertThat(suspension.suspendedOrSuspend(), is(false));
  }

  @Test
  public void testMultipleSuspendsPartialResume() {
    suspension.suspend();
    suspension.suspend();
    suspension.suspend();
    assertThat(suspension.suspendedOrSuspend(), is(true));

    suspension.resume();
    suspension.resume();
    assertThat(suspension.suspendedOrSuspend(), is(true));
  }

  @Test
  public void testSuspendedOrSuspendTwoThreads()
      throws InterruptedException, BrokenBarrierException {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final Thread thread =
        new Thread(
            () -> {
              try {
                assertThat(suspension.suspendedOrSuspend(), is(false));
                assertThat(suspension.suspendedOrSuspend(), is(true));
                barrier.await();
                barrier.await();
                suspension.resume();
              } catch (Exception e) {
                fail(e.getMessage());
              }
            });

    assertThat(suspension.suspendedOrSuspend(), is(false));
    thread.start();
    barrier.await();
    assertThat(suspension.suspendedOrSuspend(), is(true));
    barrier.await();

    thread.join();
    assertThat(thread.isAlive(), is(false));
  }

  @Test
  public void testSuspendedOrSuspendMultipleThreads()
      throws InterruptedException, BrokenBarrierException {
    final Thread[] threads = new Thread[5];
    final CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  assertThat(suspension.suspendedOrSuspend(), is(false));
                  assertThat(suspension.suspendedOrSuspend(), is(true));
                  barrier.await();
                  suspension.resume();
                } catch (Exception e) {
                  fail(e.getMessage());
                }
              });
      threads[i].start();
    }

    barrier.await();
    barrier.await();

    for (final Thread t : threads) {
      t.join();
      assertThat(t.isAlive(), is(false));
    }
  }

  @Test
  public void testSuspendTwoThreads() throws InterruptedException, BrokenBarrierException {
    final CyclicBarrier barrier = new CyclicBarrier(2);
    final Thread thread =
        new Thread(
            () -> {
              try {
                suspension.suspend();
                suspension.suspend();
                assertThat(suspension.suspendedOrSuspend(), is(true));
                barrier.await();
                barrier.await();
                suspension.resume();
                suspension.resume();
                assertThat(suspension.suspendedOrSuspend(), is(false));
              } catch (Exception e) {
                fail(e.getMessage());
              }
            });

    suspension.suspend();
    suspension.suspend();
    assertThat(suspension.suspendedOrSuspend(), is(true));
    thread.start();
    barrier.await();
    suspension.suspend();
    assertThat(suspension.suspendedOrSuspend(), is(true));
    barrier.await();

    thread.join();
    assertThat(thread.isAlive(), is(false));
  }

  @Test
  public void testSuspendMultipleThreads() throws InterruptedException, BrokenBarrierException {
    final Thread[] threads = new Thread[5];
    final CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  suspension.suspend();
                  suspension.suspend();
                  assertThat(suspension.suspendedOrSuspend(), is(true));
                  barrier.await();
                  suspension.resume();
                  suspension.resume();
                  assertThat(suspension.suspendedOrSuspend(), is(false));
                } catch (Exception e) {
                  fail(e.getMessage());
                }
              });
      threads[i].start();
    }

    barrier.await();
    barrier.await();

    for (Thread t : threads) {
      t.join();
      assertThat(t.isAlive(), is(false));
    }
  }

  @Test
  public void testThreadSuspendVectorGrowth() throws InterruptedException, BrokenBarrierException {
    final Thread[] threads = new Thread[100];
    final CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  assertThat(suspension.suspendedOrSuspend(), is(false));
                  assertThat(suspension.suspendedOrSuspend(), is(true));
                  barrier.await();
                  suspension.resume();
                } catch (Exception e) {
                  fail(e.getMessage());
                }
              });
      threads[i].start();
    }

    barrier.await();
    barrier.await();

    for (Thread t : threads) {
      t.join();
      assertThat(t.isAlive(), is(false));
    }
  }

  @Test
  public void testSuspendVectorGrowth() throws InterruptedException, BrokenBarrierException {
    final Thread[] threads = new Thread[100];
    final CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  suspension.suspend();
                  suspension.suspend();
                  assertThat(suspension.suspendedOrSuspend(), is(true));
                  barrier.await();
                  suspension.resume();
                  suspension.resume();
                  assertThat(suspension.suspendedOrSuspend(), is(false));
                } catch (Exception e) {
                  fail(e.getMessage());
                }
              });
      threads[i].start();
    }

    barrier.await();
    barrier.await();

    for (Thread t : threads) {
      t.join();
      assertThat(t.isAlive(), is(false));
    }
  }
}
