package org.jboss.as.host.controller.mgmt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SimpleFuture<V> implements Future<V> {

    private V value;
    private volatile boolean done;
    private final Lock lock = new ReentrantLock();
    private final Condition hasValue = lock.newCondition();

    /**
     * Always returns <code>false</code>
     *
     * @return <code>false</code>
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {

        lock.lock();
        try {
            while (!done) {
                hasValue.await();
            }
            return value;
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

        long deadline = unit.toMillis(timeout) + System.currentTimeMillis();
        lock.lock();
        try {
            while (!done) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException();
                }
                hasValue.await(remaining, TimeUnit.MILLISECONDS);
            }
            return value;
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * Always returns <code>false</code>
     *
     * @return <code>false</code>
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    public void set(V value) {
        lock.lock();
        try {
            this.value = value;
            done = true;
            hasValue.signalAll();
        }
        finally {
            lock.unlock();
        }
    }
}