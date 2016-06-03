package org.jboss.as.ee.component.deployers;

import java.util.concurrent.CountDownLatch;

/**
 * Countdown tracker with capabilities similar to SE CountDownLatch, but allowing threads
 * to mark and unmark themselves as privileged. Privileged threads, when entering await method,
 * will immediately proceed without checking latch's state. This reentrant behaviour allows to work around situations
 * where there is a possibility of a deadlock.
 * @author Fedor Gavrilov
 */
public final class StartupCountdown {
  private static final ThreadLocal<Boolean> isPrivileged = new ThreadLocal<Boolean>();

  private final CountDownLatch latch;

  public StartupCountdown(int count) {
    this.latch = new CountDownLatch(count);
  }

  public void countDown() {
    latch.countDown();
  }

  public void await() throws InterruptedException {
    if (Boolean.TRUE.equals(isPrivileged.get())) return;
    latch.await();
  }

  public void markAsPrivileged() {
    isPrivileged.set(Boolean.TRUE);
  }

  public void unmarkAsPrivileged() {
    isPrivileged.set(Boolean.FALSE);
  }
}
