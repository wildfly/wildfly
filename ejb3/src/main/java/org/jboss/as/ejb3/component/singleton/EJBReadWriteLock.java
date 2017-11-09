/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ejb3.component.singleton;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.as.ejb3.logging.EjbLogger;

/**
 * An implementation of {@link java.util.concurrent.locks.ReadWriteLock} which throws an {@link javax.ejb.IllegalLoopbackException}
 * when a thread holding a read lock tries to obtain a write lock.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public class EJBReadWriteLock implements ReadWriteLock, Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Keep track of the number of read locks held by this thread
     */
    private final ThreadLocal<Integer> readLockCount = new ThreadLocal<Integer>();

    /**
     * We delegate all locking semantics to this {@link java.util.concurrent.locks.ReentrantReadWriteLock}
     */
    private final ReentrantReadWriteLock delegate = new ReentrantReadWriteLock();

    /**
     * Read lock instance which will be handed out to clients
     * on a call to {@link #readLock()}
     */
    private final Lock readLock = new ReadLock();

    /**
     * Write lock instance which will be handed out to clients
     * on a call to {@link #writeLock()}
     */
    private final Lock writeLock = new WriteLock();

    /**
     * A read lock which increments/decrements the count of
     * read locks held by the thread and delegates the locking
     * calls to the {@link #delegate}
     *
     * @author Jaikiran Pai
     * @version $Revision: $
     */
    public class ReadLock implements Lock, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Delegate the call to the internal {@link java.util.concurrent.locks.ReentrantReadWriteLock} instance
         * and then increment the read lock count held by the thread
         */
        @Override
        public void lock() {
            delegate.readLock().lock();
            incReadLockCount();
        }

        /**
         * Delegate the call to the internal {@link java.util.concurrent.locks.ReentrantReadWriteLock} instance
         * and then increment the read lock count held by the thread
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            delegate.readLock().lockInterruptibly();
            incReadLockCount();
        }

        /**
         * No implementation provided
         *
         * @throws UnsupportedOperationException
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Delegate the call to the internal {@link java.util.concurrent.locks.ReentrantReadWriteLock} instance
         * and then on successful acquisition of lock, increment the read lock count held by the thread
         */
        @Override
        public boolean tryLock() {
            if (delegate.readLock().tryLock()) {
                incReadLockCount();
                return true;
            }
            return false;
        }

        /**
         * Delegate the call to the internal {@link java.util.concurrent.locks.ReentrantReadWriteLock} instance
         * and then on successful acquisition of lock, increment the read lock count held by the thread
         */
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            if (delegate.readLock().tryLock(time, unit)) {
                incReadLockCount();
                return true;
            }
            return false;
        }

        /**
         * Delegate the call to the internal {@link java.util.concurrent.locks.ReentrantReadWriteLock} instance
         * and then decrement the read lock count held by the thread
         */
        @Override
        public void unlock() {
            delegate.readLock().unlock();
            decReadLockCount();
        }

    }

    /**
     * An implementation of lock which first checks the number of {@link ReadLock}
     * held by this thread. If the thread already holds a {@link ReadLock}, then
     * this implementation throws an {@link javax.ejb.IllegalLoopbackException} when a lock
     * is requested
     *
     * @author Jaikiran Pai
     * @version $Revision: $
     */
    public class WriteLock implements Lock, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Ensures that the current thread doesn't hold any read locks. If
         * the thread holds any read locks, this method throws a {@link javax.ejb.IllegalLoopbackException}.
         * If no read locks are held, then this method delegates the call to the
         * internal delegate {@link java.util.concurrent.locks.ReentrantReadWriteLock}
         */
        @Override
        public void lock() {
            checkLoopback();
            delegate.writeLock().lock();
        }

        /**
         * Ensures that the current thread doesn't hold any read locks. If
         * the thread holds any read locks, this method throws a {@link javax.ejb.IllegalLoopbackException}.
         * If no read locks are held, then this method delegates the call to the
         * internal delegate {@link java.util.concurrent.locks.ReentrantReadWriteLock}
         */
        @Override
        public void lockInterruptibly() throws InterruptedException {
            checkLoopback();
            delegate.writeLock().lockInterruptibly();
        }

        /**
         * Not implemented
         *
         * @throws UnsupportedOperationException
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Ensures that the current thread doesn't hold any read locks. If
         * the thread holds any read locks, this method throws a {@link javax.ejb.IllegalLoopbackException}.
         * If no read locks are held, then this method delegates the call to the
         * internal delegate {@link java.util.concurrent.locks.ReentrantReadWriteLock}
         */
        @Override
        public boolean tryLock() {
            checkLoopback();
            return delegate.writeLock().tryLock();
        }

        /**
         * Ensures that the current thread doesn't hold any read locks. If
         * the thread holds any read locks, this method throws a {@link javax.ejb.IllegalLoopbackException}.
         * If no read locks are held, then this method delegates the call to the
         * internal delegate {@link java.util.concurrent.locks.ReentrantReadWriteLock}
         */
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            checkLoopback();
            return delegate.writeLock().tryLock(time, unit);
        }

        /**
         * This method delegates the call to the
         * internal delegate {@link java.util.concurrent.locks.ReentrantReadWriteLock}
         */
        @Override
        public void unlock() {
            delegate.writeLock().unlock();
        }
    }

    /**
     * Ensures that the current thread doesn't hold any read locks. If
     * the thread holds any read locks, this method throws a {@link javax.ejb.IllegalLoopbackException}.
     */
    private void checkLoopback() {
        Integer current = readLockCount.get();
        if (current != null) {
            assert current.intValue() > 0 : "readLockCount is set, but to 0";
            throw EjbLogger.ROOT_LOGGER.failToUpgradeToWriteLock();
        }
    }

    /**
     * Decrements the read lock count held by the thread
     */
    private void decReadLockCount() {
        Integer current = readLockCount.get();
        int next;
        assert current != null : "can't decrease, readLockCount is not set";
        next = current.intValue() - 1;
        if (next == 0)
            readLockCount.remove();
        else
            readLockCount.set(new Integer(next));
    }

    /**
     * Increments the read lock count held by the thread
     */
    private void incReadLockCount() {
        Integer current = readLockCount.get();
        int next;
        if (current == null)
            next = 1;
        else
            next = current.intValue() + 1;
        readLockCount.set(new Integer(next));
    }

    /**
     * @see java.util.concurrent.locks.ReadWriteLock#readLock()
     */
    @Override
    public Lock readLock() {
        return readLock;
    }

    /**
     * @see java.util.concurrent.locks.ReadWriteLock#writeLock()
     */
    @Override
    public Lock writeLock() {
        return writeLock;
    }
}
