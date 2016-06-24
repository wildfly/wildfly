package org.jboss.as.ee.component.deployers;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Countdown tracker with capabilities similar to SE CountDownLatch, but allowing threads
 * to mark and unmark themselves as privileged and allowing counting up. Privileged threads, when entering await method,
 * will immediately proceed without checking latch's state. This reentrant behaviour allows to work around situations
 * where there is a possibility of a deadlock.
 * @author Fedor Gavrilov
 */
public final class StartupCountdown {
  private final ThreadLocal<Boolean> isPrivileged = new ThreadLocal<Boolean>();
  private final AbstractQueuedSynchronizer counter = new AbstractQueuedSynchronizer() {
    @Override
    protected int tryAcquireShared(final int acquires) {
      return getState() == 0 ? 1 : -1;
    }

    // passed arg could only be +1 or -1 to avoid deadlock
    @Override
    protected boolean tryReleaseShared(final int modification) {
      while (true) {
        int state = getState();
        int newState = state - modification;
        if (compareAndSetState(state, newState)) return newState == 0;
      }
    }
  };

  public void increment(final int n) {
    if (n < 0) throw new IllegalArgumentException();
    for (int i = n; i > 0; i--) counter.releaseShared(-1);
  }

  public void decrement() {
    counter.releaseShared(1);
  }

  public void await() throws InterruptedException {
    if (Boolean.TRUE.equals(isPrivileged.get())) return;
    counter.acquireSharedInterruptibly(1);
  }

  public void markAsPrivileged() {
    isPrivileged.set(Boolean.TRUE);
  }

  public void unmarkAsPrivileged() {
    isPrivileged.set(Boolean.FALSE);
  }
}
