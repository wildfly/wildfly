/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.ejb3.concurrency;

import org.jboss.as.ejb3.component.singleton.EJBReadWriteLock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.IllegalLoopbackException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Tests the {@link EJBReadWriteLock}
 *
 * @author Jaikiran Pai
 * @version $Revision: $
 */
public class EJBReadWriteLockTest {

    /**
     * Used in tests
     */
    private EJBReadWriteLock ejbReadWriteLock;

    @Before
    public void beforeTest() {
        this.ejbReadWriteLock = new EJBReadWriteLock();

    }

    @After
    public void afterTest() {
        this.ejbReadWriteLock = null;
    }

    /**
     * Test that a {@link javax.ejb.IllegalLoopbackException} is thrown when the thread owning a read lock
     * tries to obtain a write lock
     *
     * @throws Exception
     */
    @Test
    public void testIllegalLoopBack() throws Exception {
        // get a read lock
        Lock readLock = this.ejbReadWriteLock.readLock();

        // lock it!
        readLock.lock();

        // now get a write lock and try to lock it (should fail with IllegalLoopBack)
        Lock writeLock = this.ejbReadWriteLock.writeLock();
        try {
            writeLock.lock();
            // unlock the (unexpected obtained lock) and then fail the testcase
            writeLock.unlock();

            Assert.fail("Unexpected acquired write lock");

        } catch (IllegalLoopbackException ilbe) {
            // expected
        } finally {
            // unlock the write lock
            readLock.unlock();
        }

    }

    /**
     * Test that when a thread tries to obtain a read lock when another thread holds a write lock,
     * fails to acquire the lock, if the write lock is not released within the timeout specified
     *
     * @throws Exception
     */
    @Test
    public void testTimeout() throws Exception {
        // we use a countdown latch for the 2 threads involved
        CountDownLatch latch = new CountDownLatch(2);
        // get a write lock
        Lock writeLock = this.ejbReadWriteLock.writeLock();
        // create a thread which will get hold of a write lock
        // and do some processing for 5 seconds
        Thread threadHoldingWriteLock = new Thread(new ThreadHoldingWriteLock(latch, writeLock, 5000));

        // get a read lock
        Lock readLock = this.ejbReadWriteLock.readLock();

        // start the write lock thread (which internally will obtain
        // a write lock and start a 5 second processing)
        threadHoldingWriteLock.start();
        // wait for few milli sec for the write lock thread to obtain a write lock
        Thread.sleep(500);
        // now try and get a read lock, *shouldn't* be able to obtain the lock
        // before the 2 second timeout
        try {
            // try a read lock with 2 second timeout
            boolean readLockAcquired = readLock.tryLock(2, TimeUnit.SECONDS);
            Assert.assertFalse("Unexpected obtained a read lock", readLockAcquired);
        } catch (ConcurrentAccessTimeoutException cate) {
            // expected
        } finally {
            // let the latch know that this thread is done with its part
            // of processing
            latch.countDown();

            // now let's wait for the other thread to complete processing
            // and bringing down the count on the latch
            latch.await();
        }


    }

    /**
     * Tests that a thread can first get a write lock and at a later point in time, get
     * a read lock
     *
     * @throws Exception
     */
    @Test
    public void testSameThreadCanGetWriteThenReadLock() throws Exception {
        Lock writeLock = this.ejbReadWriteLock.writeLock();
        // lock it!
        writeLock.lock();

        Lock readLock = this.ejbReadWriteLock.readLock();
        // lock it! (should work, because we are going from a write to read and *not*
        // the other way round)
        try {
            boolean readLockAcquired = readLock.tryLock(2, TimeUnit.SECONDS);
            // unlock the read lock, because we don't need it anymore
            if (readLockAcquired) {
                readLock.unlock();
            }
            Assert.assertTrue("Could not obtain read lock when write lock was held by the same thread!", readLockAcquired);
        } finally {
            // unlock our write lock
            writeLock.unlock();
        }

    }

    /**
     * An implementation of {@link Runnable} which in its {@link #run()} method
     * will first obtain a lock and then will go to sleep for the specified amount
     * of time. After processing, it will unlock the {@link java.util.concurrent.locks.Lock}
     *
     * @author Jaikiran Pai
     * @version $Revision: $
     */
    private class ThreadHoldingWriteLock implements Runnable {
        /**
         * Lock
         */
        private Lock lock;

        /**
         * The amount of time, in milliseconds, this {@link ThreadHoldingWriteLock}
         * will sleep for in its {@link #run()} method
         */
        private long processingTime;

        /**
         * A latch for notifying any waiting threads
         */
        private CountDownLatch latch;

        /**
         * Creates a {@link ThreadHoldingWriteLock}
         *
         * @param latch          A latch for notifying any waiting threads
         * @param lock           A lock that will be used for obtaining a lock during processing
         * @param processingTime The amount of time in milliseconds, this thread will sleep (a.k.a process)
         *                       in its {@link #run()} method
         */
        public ThreadHoldingWriteLock(CountDownLatch latch, Lock lock, long processingTime) {
            this.lock = lock;
            this.processingTime = processingTime;
            this.latch = latch;
        }

        /**
         * Obtains a lock, sleeps for {@link #processingTime} milliseconds and then unlocks the lock
         *
         * @see Runnable#run()
         */
        @Override
        public void run() {
            // lock it!
            this.lock.lock();
            // process(sleep) for the specified time
            try {
                Thread.sleep(this.processingTime);
            } catch (InterruptedException e) {
                // ignore
            } finally {
                // unlock
                this.lock.unlock();
                // let any waiting threads know that we are done processing
                this.latch.countDown();
            }
        }
    }


}
