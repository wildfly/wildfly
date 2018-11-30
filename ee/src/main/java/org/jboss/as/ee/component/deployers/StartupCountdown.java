package org.jboss.as.ee.component.deployers;

import java.util.LinkedList;
import java.util.Queue;

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
  private volatile boolean startupFailed = false;
  private Queue<StartupCallback> callbacks = new LinkedList<>();

  public StartupCountdown(int count) {
    this.count = count;
  }

  public void startupFailure() {
    synchronized (this) {
      startupFailed = true;
      try {
        while (!callbacks.isEmpty()) {
          callbacks.poll().onFailure();
        }
      } finally {
        notifyAll();
      }
    }
  }

  public void startupSuccessful() {
    synchronized (this) {
      if (-- count == 0) {
        try {
          while (!callbacks.isEmpty()) {
            callbacks.poll().onSuccess();
          }
        } finally {
          notifyAll();
        }
      }
    }
  }

  /*
   * use {@code startupSuccessful}/{@code startupFailure} instead
   */
  @Deprecated
  public void countDown() {
    startupSuccessful();
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

  /**
   * Executes a lightweight action when the countdown reaches 0.
   * If StartupCountdown is not at zero when the method is called, passed callback will be executed by the last thread to call countDown.
   * If StartupCountdown is at zero already, passed callback will be executed immediately by the caller thread.
   * @param callback to execute. Should not be null.
   */
  @Deprecated
  public void addCallback(final Runnable callback) {
    addCallback(new StartupCallback() {
      @Override
      public void onSuccess() {
        callback.run();
      }

      @Override
      public void onFailure() {
        // do nothing by default
      }
    });
  }

  public void addCallback(final StartupCallback callback) {
    synchronized (this) {
      if (count == 0) {
        callback.onSuccess();
      } else if (startupFailed) {
        callback.onFailure();
      } else {
        callbacks.add(callback);
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

  public interface StartupCallback {
    void onSuccess();

    void onFailure();
  }
}
