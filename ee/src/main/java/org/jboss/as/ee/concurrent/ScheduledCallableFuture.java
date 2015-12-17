package org.jboss.as.ee.concurrent;

import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * Class that allows callables to be scheduled as a Runnable, which allows suspend queuing to work correctly.
 *
 * @author Stuart Douglas
 */
public class ScheduledCallableFuture<V> implements ScheduledFuture<V> {

    private final Callable<V> callable;
    private ScheduledFuture underlying;
    private V value;
    private boolean done;
    private boolean cancelled;
    private Exception exception;

    public ScheduledCallableFuture(Callable<V> callable) {
        this.callable = callable;
    }

    public Runnable getTask() {
        Runnable task = () -> {
            try {
                final V result = callable.call();
                complete(result);
            } catch (Exception e) {
                error(e);
            }
        };
        if(callable instanceof ManagedTask) {
            return new ManagedRunnable(task);
        } else {
            return task;
        }

    }

    void setUnderlying(ScheduledFuture underlying) {
        this.underlying = underlying;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return underlying.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return underlying.compareTo(o);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        cancel();
        if(done) {
            return false;
        }
        return underlying.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return underlying.isCancelled();
    }

    @Override
    public boolean isDone() {
        return underlying.isDone();
    }

    private synchronized void complete(V value) {
        done = true;
        this.value = value;
        notifyAll();
    }

    private synchronized void error(Exception e) {
        done = true;
        this.exception = e;
        notifyAll();
    }

    private synchronized void cancel() {
        this.cancelled = true;
        this.done = true;
        notifyAll();
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        while (!done) {
            wait();
        }
        if(cancelled) {
            throw new CancellationException();
        }
        if(exception != null) {
            throw new ExecutionException(exception);
        }
        return value;
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (System.currentTimeMillis() < end) {
            wait(end - System.currentTimeMillis());
        }
        if(!done) {
            throw new TimeoutException();
        }
        if(cancelled) {
            throw new CancellationException();
        }
        if(exception != null) {
            throw new ExecutionException(exception);
        }
        return value;
    }


    private class ManagedRunnable implements Runnable, ManagedTask {

        private final Runnable runnable;

        private ManagedRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Map<String, String> getExecutionProperties() {
            return ((ManagedTask)callable).getExecutionProperties();
        }

        @Override
        public ManagedTaskListener getManagedTaskListener() {
            return ((ManagedTask)callable).getManagedTaskListener();
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
