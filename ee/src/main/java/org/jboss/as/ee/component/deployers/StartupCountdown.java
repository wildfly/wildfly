package org.jboss.as.ee.component.deployers;

/**
 * Countdown tracker with capabilities similar to SE CountDownLatch, but allowing threads
 * to mark and unmark themselves as privileged. Privileged threads, when entering await method,
 * will immediately proceed without checking latch's state. This reentrant behaviour allows to work around situations
 * where there is a possibility of a deadlock.
 * @author Fedor Gavrilov
 */
public final class StartupCountdown {
  private static final ThreadLocal<Frame> frames = new ThreadLocal<>();

  private volatile int count;

  public StartupCountdown(int count) {
    this.count = count;
  }

  public void countDown() {
    synchronized (this) {
      if (-- count == 0) {
        notifyAll();
      }
    }
  }

  public void countUp(final int count) {
    synchronized (this) {
      this.count += count;
    }
  }

  public void await() throws InterruptedException {
    if (isPrivileged()) return;
    if (count != 0) {
      synchronized (this) {
        while (count != 0) wait();
      }
    }
  }

  public boolean isPrivileged() {
    final Frame frame = frames.get();
    return frame != null && frame.contains(this);
  }

  public Frame enter() {
    final Frame frame = frames.get();
    frames.set(new Frame(frame, this));
    return frame;
  }

  public static Frame current() {
    return frames.get();
  }

  public static void restore(Frame frame) {
    frames.set(frame);
  }

  public static final class Frame {
    private final Frame prev;
    private final StartupCountdown countdown;

    Frame(final Frame prev, final StartupCountdown countdown) {
      this.prev = prev;
      this.countdown = countdown;
    }

    boolean contains(StartupCountdown countdown) {
      return countdown == this.countdown || prev != null && prev.contains(countdown);
    }
  }
}
