/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.remote;

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

    void set(V value) {
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