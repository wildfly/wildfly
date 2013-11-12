/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Basic lock implementation using a permit object to allow reentrancy. The lock will only be released when all
 * participants which previously acquired the lock have called {@linkplain #unlock}.
 *
 * @author Emanuel Muckenhuber
 */
class ModelControllerLock {
    private final Sync sync = new Sync();

    void lock(Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquire(permit);
    }

    void lockInterruptibly(Integer permit) throws InterruptedException {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.acquireInterruptibly(permit);
    }

    void unlock(Integer permit) {
        if (permit == null) {
            throw new IllegalArgumentException();
        }
        sync.release(permit);
    }

    boolean detectDeadlockAndGetLock(int permit) {
        return sync.tryAcquire(permit);
    }

    private class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        private final AtomicReference<Object> permitHolder = new AtomicReference<Object>(null);

        @Override
        protected synchronized boolean tryAcquire(int permit) {
            final int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, 1)) {
                    permitHolder.set(permit);
                    return true;
                }
            } else if (permitHolder.get().equals(permit)) {
                for (;;) {
                    int current = getState();
                    int next = current + 1; // increase by one
                    if (next < 0) // overflow
                        throw new Error("Maximum lock count exceeded");
                    if (compareAndSetState(current, next)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected synchronized boolean tryRelease(int permit) {
            final Object value = permitHolder.get();
            if (value == null) {
                throw new IllegalStateException();
            }
            if (value.equals(permit)) {
                for (;;) {
                    int current = getState();
                    int next = current - 1; // count down one
                    if(next < 0)
                        throw new IllegalStateException();
                    if (compareAndSetState(current, next)) {
                        if (next == 0) {
                            permitHolder.compareAndSet(value, null);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
            return false;
        }
    }
}

