package ch.usi.inf.profiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  public void testSimpleSuspendAndResume() {
    assertFalse(suspension.suspend());
    suspension.resume();
  }

  @Test
  public void testMultipleSuspendsPartialResume() {
    assertFalse(suspension.suspend());
    assertTrue(suspension.suspend());
    assertTrue(suspension.suspend());

    suspension.resume();
    suspension.resume();
    assertTrue(suspension.suspend());
  }

  @Test
  public void testNewThreadInsertionLogic() throws InterruptedException, BrokenBarrierException {
    Thread[] threads = new Thread[5];
    CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  assertFalse(suspension.suspend());
                  assertTrue(suspension.suspend());
                  barrier.await();
                  suspension.resume();
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
      assertFalse(t.isAlive());
    }
  }

  @Test
  public void testThreadSuspendVectorGrowth() throws InterruptedException, BrokenBarrierException {
    Thread[] threads = new Thread[100];
    CyclicBarrier barrier = new CyclicBarrier(threads.length + 1);

    for (int i = 0; i < threads.length; ++i) {
      threads[i] =
          new Thread(
              () -> {
                try {
                  barrier.await();
                  assertFalse(suspension.suspend());
                  assertTrue(suspension.suspend());
                  barrier.await();
                  suspension.resume();
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
      assertFalse(t.isAlive());
    }
  }
}
