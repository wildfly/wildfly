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

package org.jboss.as.clustering.lock;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;

import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 * Distributed lock manager that supports a single, exclusive cluster wide lock or concurrent locally-exclusive locks on the
 * various cluster nodes. A request to acquire a local lock will block while the cluster-wide lock is held or while another
 * thread holds the local lock, and a request to acquire the cluster-wide lock will block while the local lock is held on any
 * node.
 * <p>
 * Users should not attempt to acquire the cluster-wide lock while holding a local lock or vice versa; failure to respect this
 * requirement may lead to deadlocks.
 * </p>
 *
 * @author Brian Stansberry
 *
 * @version $Revision:$
 */
public class LocalAndClusterLockManager {

    class LocalLock {
        volatile ClusterNode holder;
        private final AtomicBoolean locked = new AtomicBoolean(false);
        private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();

        void lock(ClusterNode caller, long timeout) throws TimeoutException {
            long deadline = System.currentTimeMillis() + timeout;
            boolean wasInterrupted = false;
            Thread current = Thread.currentThread();
            waiters.add(current);

            try {
                // Block while not first in queue or cannot acquire lock
                while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
                    LockSupport.parkUntil(deadline);
                    if (Thread.interrupted()) // ignore interrupts while waiting
                        wasInterrupted = true;
                    if (System.currentTimeMillis() >= deadline) {
                        if (waiters.peek() != current || !locked.compareAndSet(false, true)) {
                            throw new TimeoutException(this.holder);
                        }
                        break;
                    }
                }

                if (locked.get()) {
                    holder = caller;
                } else {
                    throw new TimeoutException(this.holder);
                }
            } finally {
                waiters.remove();
                if (wasInterrupted) // reassert interrupt status on exit
                    current.interrupt();
            }
        }

        void unlock(ClusterNode caller) {
            if (caller.equals(holder)) {
                locked.set(false);
                holder = null;
                LockSupport.unpark(waiters.peek());
            }
        }
    }

    /** Handles callbacks from the cluster lock support object */
    class ClusterHandler implements LocalLockHandler {
        // ----------------------------------------------------- LocalLockHandler

        @Override
        public ClusterNode getLocalNode(ClusterNode localNode) {
            return LocalAndClusterLockManager.this.localNode;
        }

        @Override
        public void setLocalNode(ClusterNode localNode) {
            LocalAndClusterLockManager.this.localNode = localNode;
        }

        @Override
        public void lockFromCluster(Serializable lockName, ClusterNode caller, long timeout) throws TimeoutException,
                InterruptedException {
            LocalAndClusterLockManager.this.doLock(lockName, caller, timeout);
        }

        @Override
        public ClusterNode getLockHolder(Serializable lockName) {
            LocalLock lock = LocalAndClusterLockManager.this.getLocalLock(lockName, false);
            return lock == null ? null : lock.holder;
        }

        @Override
        public void unlockFromCluster(Serializable lockName, ClusterNode caller) {
            LocalAndClusterLockManager.this.doUnlock(lockName, caller);
        }

    }

    ClusterNode localNode;
    private ConcurrentMap<Serializable, LocalLock> localLocks = new ConcurrentHashMap<Serializable, LocalLock>();
    private final NonGloballyExclusiveClusterLockSupport clusterSupport;

    public LocalAndClusterLockManager(String serviceHAName, GroupRpcDispatcher rpcDispatcher,
            GroupMembershipNotifier membershipNotifier) {
        ClusterHandler handler = new ClusterHandler();
        clusterSupport = new NonGloballyExclusiveClusterLockSupport(serviceHAName, rpcDispatcher, membershipNotifier, handler);
    }

    // ----------------------------------------------------------------- Public

    /**
     * Acquires a local lock, blocking if another thread is holding it or if the global, cluster-wide lock is held.
     *
     * @param lockName unique name identifying the lock to acquire
     * @param timeout max number of ms to wait to acquire the lock before failing with a TimeoutException
     *
     * @throws TimeoutException if the lock cannot be acquired before <code>timeout</code> ms have elapsed
     * @throws InterruptedException if the thread is interrupted
     */
    public void lockLocally(Serializable lockName, long timeout) throws TimeoutException, InterruptedException {
        doLock(lockName, this.localNode, timeout);
    }

    /**
     * Releases a previously acquired local lock.
     *
     * @param lockName unique name identifying the lock to release
     */
    public void unlockLocally(Serializable lockName) {
        doUnlock(lockName, this.localNode);
    }

    /**
     * Acquires a globally exclusive cluster-wide lock, blocking if another thread is holding it locally, another node is
     * holding it, or if the local lock is held on any node in the cluster.
     *
     * @param lockName unique name identifying the lock to acquire
     * @param timeout max number of ms to wait to acquire the lock before failing with a TimeoutException
     *
     * @throws TimeoutException if the lock cannot be acquired before <code>timeout</code> ms have elapsed
     * @throws InterruptedException if the thread is interrupted
     */
    public void lockGlobally(Serializable lockName, long timeout) throws TimeoutException, InterruptedException {
        if (!this.clusterSupport.lock(lockName, timeout)) {
            throw new TimeoutException(MESSAGES.cannotAcquireLock(lockName));
        }
    }

    /**
     * Releases a previously acquired local lock.
     *
     * @param lockName unique name identifying the lock to release
     */
    public void unlockGlobally(Serializable lockName) {
        this.clusterSupport.unlock(lockName);
    }

    /**
     * Brings this object to a state where it is ready for normal operation.
     *
     * @throws Exception
     */
    public void start() throws Exception {
        this.clusterSupport.start();

        if (this.localNode == null) {
            throw MESSAGES.nullVar("localNode");
        }
    }

    /**
     * Removes this object from a state where it is ready for normal operation and performs cleanup work.
     *
     * @throws Exception
     */
    public void stop() throws Exception {
        this.clusterSupport.stop();
    }

    // ----------------------------------------------------------------- Private

    LocalLock getLocalLock(Serializable categoryName, boolean create) {
        LocalLock category = localLocks.get(categoryName);
        if (category == null && create) {
            category = new LocalLock();
            LocalLock existing = localLocks.putIfAbsent(categoryName, category);
            if (existing != null) {
                category = existing;
            }
        }
        return category;
    }

    void doLock(Serializable lockName, ClusterNode caller, long timeout) throws TimeoutException {
        LocalLock lock = getLocalLock(lockName, true);
        lock.lock(caller, timeout);
    }

    void doUnlock(Serializable lockName, ClusterNode caller) {
        LocalLock lock = getLocalLock(lockName, false);
        if (lock != null) {
            lock.unlock(caller);
        }
    }

}
