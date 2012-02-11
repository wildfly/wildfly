/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;

import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 * Distributed lock manager intended for use cases where multiple local threads can share the lock, but only one node in the
 * cluster can have threads using the lock. Nodes holding the lock yield it to remote requestors if no local threads are using
 * it; otherwise remote requestors block.
 * <p>
 * The expected use case for this class is controlling access to resources that are typically only accessed on a single node
 * (e.g. web sessions or stateful session beans), with the distributed lock used primarily to guarantee that.
 * </p>
 * @author Brian Stansberry
 */
public class SharedLocalYieldingClusterLockManager {
    /** Result of a {@link SharedLocalYieldingClusterLockManager#lock(Serializable, long, boolean) lock call} */
    public static enum LockResult {
        /** Indicates the lock was acquired after requesting it from the cluster */
        ACQUIRED_FROM_CLUSTER,
        /** Indicates this node already held the lock */
        ALREADY_HELD,
        /**
         * Indicates the 'newLock' param passed to {@link YieldingClusterLockManager#lock(Serializable, long, boolean)} was
         * <code>true</code> and the local node in fact was unaware of the lock. If in fact the local node was already aware of
         * the lock (which would generally indicate a flaw in the application using this class) NEW_LOCK will not be returned;
         * rather one of the other enum values will be returned.
         */
        NEW_LOCK
    }

    class LocalLock {
        volatile boolean removable;
        private final Queue<Thread> waiters = new ConcurrentLinkedQueue<Thread>();
        final AtomicReference<LockState> lockState = new AtomicReference<LockState>(LockState.AVAILABLE);

        /**
         * Just takes the lock for the local node. This should only be invoked for new locks or in a callback from the cluster
         * support, which won't make the callback until all other nodes in cluster agree.
         * @return the LockState after the lock is taken.
         */
        LockState lockForLocalNode() {
            LockState lockedState = null;
            for (;;) {
                LockState current = lockState.get();
                lockedState = current.takeLocal(SharedLocalYieldingClusterLockManager.this.localNode);
                if (lockState.compareAndSet(current, lockedState)) {
                    break;
                }
            }
            return lockedState;
        }

        /**
         * Attempts to take the lock, blocking if it has an owner
         * @param caller node that wants the lock
         * @param timeout max ms to wait to acquire lock
         * @return the LockState after the lock is taken
         * @throws TimeoutException if the lock cannot be acquired within <code>timeout</code> ms
         */
        LockState lockForRemoteNode(ClusterNode caller, long timeout) throws TimeoutException {
            LockState lockedState = null;

            long deadline = System.currentTimeMillis() + timeout;
            boolean wasInterrupted = false;
            Thread currentThread = Thread.currentThread();
            waiters.add(currentThread);

            try {
                // Block while not first in queue or cannot acquire lock
                LockState currentState = lockState.get();
                lockedState = currentState.takeRemote(caller);
                while (waiters.peek() != currentThread
                        || currentState.lockHolder == SharedLocalYieldingClusterLockManager.this.localNode
                        || !lockState.compareAndSet(currentState, lockedState)) {
                    LockSupport.parkUntil(deadline);
                    if (Thread.interrupted()) // ignore interrupts while waiting
                        wasInterrupted = true;

                    currentState = lockState.get();
                    lockedState = currentState.takeRemote(caller);
                    if (System.currentTimeMillis() >= deadline) {
                        // One last attempt
                        if (waiters.peek() != currentThread
                                || currentState.lockHolder == SharedLocalYieldingClusterLockManager.this.localNode
                                || !lockState.compareAndSet(currentState, lockedState)) {
                            throw new TimeoutException(SharedLocalYieldingClusterLockManager.this.localNode);
                        }
                        // Succeeded
                        break;
                    }
                }
            } finally {
                waiters.remove();
                if (wasInterrupted) // reassert interrupt status on exit
                    currentThread.interrupt();
            }

            return lockedState;
        }

        /**
         * Decrements the lock count and removes the local node as lock holder if the count reaches zero.
         * @param caller must be the local node
         * @throws IllegalStateException if <code>caller</code> is not the local node
         */
        void unlock(ClusterNode caller) {
            LockState current = lockState.get();
            if (caller.equals(current.lockHolder)) {
                LockState newState = null;
                if (SharedLocalYieldingClusterLockManager.this.localNode == current.lockHolder) {
                    for (;;) {
                        newState = current.releaseLock();
                        if (lockState.compareAndSet(current, newState)) {
                            break;
                        }
                        current = lockState.get();
                    }
                } else {
                    throw MESSAGES.receivedUnlockForRemoteNode(caller);
                    // BES -- the below was an impl, but this case is not correct so
                    // replaced with above exception
                    // for (;;)
                    // {
                    // newState = current.release();
                    // if (lockState.compareAndSet(current, newState))
                    // {
                    // break;
                    // }
                    // else
                    // {
                    // current = lockState.get();
                    // }
                    // }
                }

                if (newState.lockHolder == null) {
                    // Wake up anyone waiting for this lock
                    LockSupport.unpark(waiters.peek());
                }
            }
        }

        LockState registerForLocalLock() {
            LockState current = lockState.get();
            LockState newState = null;
            for (;;) {
                newState = current.register(SharedLocalYieldingClusterLockManager.this.localNode);
                if (lockState.compareAndSet(current, newState)) {
                    break;
                }
                current = lockState.get();
            }
            return newState;
        }

    }

    /**
     * Immutable data object that encapsulates the state of a LocalLock. Provides methods that return LockStates for the
     * expected state transitions.
     */
    private static class LockState {
        /** The standard initial state */
        static final LockState AVAILABLE = new LockState(0, null, null, null, false);

        /** Number of local callers that have locked or want to lock */
        final int localLockCount;
        /** The node that holds the lock */
        final ClusterNode lockHolder;
        /** The node that last held the lock, if currently unheld */
        private final ClusterNode lastHolder;
        /**
         * Most recent thread to call {@link LocalLock#registerForLocalLock()}. Used by a thread that asks the cluster for the
         * lock to detect if other threads have subsequently registered interest and need to be notified of the result.
         */
        final Thread latestRegistrant;
        /** Flag indicating the lock object is no longer valid and callers should obtain a new one */
        final boolean invalid;

        private LockState(int localLockCount, ClusterNode lockHolder, ClusterNode lastHolder, Thread latestRegistrant,
                boolean invalid) {
            this.localLockCount = localLockCount;
            this.lockHolder = lockHolder;
            this.lastHolder = lastHolder;
            this.latestRegistrant = latestRegistrant;
            this.invalid = invalid;
        }

        /**
         * Record interest in obtaining the lock. If the lock is unheld and <code>registrant</code> was the last holder of the
         * lock, then <code>registrant</code> will be made holder of the lock.
         * @return a LockState with a lock count one higher than this one and with the current thread as latestRegistrant
         */
        LockState register(ClusterNode registrant) {
            ClusterNode newHolder = (lockHolder == null && lastHolder == registrant) ? registrant : lockHolder;
            ClusterNode newLast = newHolder == null ? lastHolder : null;
            return new LockState(localLockCount + 1, newHolder, newLast, Thread.currentThread(), invalid);
        }

        /**
         * Record that interest in obtaining the lock is finished. Note that does not mean release the lock. This method should
         * be called in a finally block following a call to {@link #register(org.jboss.as.clustering.ClusterNode)}.
         * @param decrement <code>true</code> if unregistering should reverse the lock count increase that occurred with {@link #register(org.jboss.as.clustering.ClusterNode)}
         * @return a LockState that will not have the calling thread as latestRegistrant and that, if <code>decrement</code> is
         *         <code>true</code> will have a lock count one less than this one
         */
        LockState unregister(boolean decrement) {
            Thread registrant = (latestRegistrant == Thread.currentThread() ? null : latestRegistrant);
            int newCount = decrement ? localLockCount - 1 : localLockCount;
            return new LockState(newCount, lockHolder, lastHolder, registrant, invalid);
        }

        /**
         * Increase the local lock count and assign the lock holder to the given node. This should only be called with the local
         * node as owner.
         * @param owner the local node.
         * @return a LockState with a lock count one higher than this one and with <code>owner</code> as the lock holder
         */
        LockState takeLocal(ClusterNode owner) {
            Thread registrant = (latestRegistrant == Thread.currentThread() ? null : latestRegistrant);
            return new LockState(localLockCount + 1, owner, null, registrant, invalid);
        }

        /**
         * Decrease the local lock count, and, if the count is now zero set the lock holder as null.
         * @return a LockState with a lock count one lower than this one and potentially with no lock holder
         */
        LockState releaseLock() {
            return localLockCount == 1 ? new LockState(0, null, lockHolder, latestRegistrant, invalid) : new LockState(
                    localLockCount - 1, lockHolder, lastHolder, latestRegistrant, invalid);
        }

        /**
         * Assign the given node as lock owner. This should only be called with a remote node as owner.
         * @param owner a remote node. Should not be <code>null</code>
         * @return a LockState with <code>owner</code> as lock holder
         */
        LockState takeRemote(ClusterNode owner) {
            return new LockState(localLockCount, owner, null, latestRegistrant, invalid);
        }

        // BES -- this would be called if it was valid for ClusterHandler.unlock
        // to be called from a remote caller
        // private LockState release()
        // {
        // return new LockState(localLockCount, null, null, latestRegistrant, invalid);
        // }

        LockState invalidate() {
            return new LockState(localLockCount, lockHolder, null, latestRegistrant, true);
        }

    }

    /**
     * Handles callbacks from the cluster lock support object. We use a separate class to avoid exposing the LocalLockHandler in
     * the top level class' API.
     */
    class ClusterHandler implements LocalLockHandler {
        // ----------------------------------------------------- LocalLockHandler

        @Override
        public ClusterNode getLocalNode(ClusterNode localNode) {
            return SharedLocalYieldingClusterLockManager.this.localNode;
        }

        @Override
        public void setLocalNode(ClusterNode localNode) {
            SharedLocalYieldingClusterLockManager.this.localNode = localNode;
        }

        @Override
        public void lockFromCluster(Serializable lockName, ClusterNode caller, long timeout) throws TimeoutException,
                InterruptedException {
            LocalLock lock = getLocalLock(lockName, true);

            if (localNode.equals(caller)) { // can't use identity here as caller may have been deserialized
                LockState lockState = lock.lockForLocalNode();
                if (lockState.latestRegistrant != Thread.currentThread()) {
                    // Someone else may be blocking waiting for this thread to
                    // acquire the lock from the cluster. Tell them we have.
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            } else {
                LockState currentState = lock.lockForRemoteNode(caller, timeout);

                // Any local thread who has a ref to lock will now need to request it
                // remotely from caller, which won't grant it until this method returns.
                // So, we can remove lock from the map. If that local thread is granted
                // the lock by caller, when that thread calls lockFromCluster, we'll create
                // a new lock to handle that.
                localLocks.remove(lockName, lock);

                // Make sure anyone with a reference to lock sees that it is no
                // longer valid
                LockState invalidated = null;
                for (;;) {
                    invalidated = currentState.invalidate();
                    if (lock.lockState.compareAndSet(currentState, invalidated)) {
                        break;
                    }
                    currentState = lock.lockState.get();
                }

                if (invalidated.latestRegistrant != null) {
                    // Wake up anyone waiting on this lock so they know it's invalid
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }

        @Override
        public ClusterNode getLockHolder(Serializable lockName) {
            LocalLock lock = getLocalLock(lockName, false);
            return lock == null ? null : lock.lockState.get().lockHolder;
        }

        @Override
        public void unlockFromCluster(Serializable lockName, ClusterNode caller) {
            LocalLock lock = getLocalLock(lockName, false);
            if (lock != null) {
                lock.unlock(caller);
            }
        }

    }

    ClusterNode localNode;
    ConcurrentMap<Serializable, LocalLock> localLocks = new ConcurrentHashMap<Serializable, LocalLock>();
    private final YieldingGloballyExclusiveClusterLockSupport clusterSupport;

    public SharedLocalYieldingClusterLockManager(String serviceHAName, GroupRpcDispatcher rpcDispatcher, GroupMembershipNotifier membershipNotifier) {
        ClusterHandler handler = new ClusterHandler();
        clusterSupport = new YieldingGloballyExclusiveClusterLockSupport(serviceHAName, rpcDispatcher, membershipNotifier,
                handler);
    }

    // ----------------------------------------------------------------- Public

    /**
     * Acquire the given lock.
     * @param lockName the identifier of the lock that should be acquired
     * @param timeout max time in ms to wait before throwing a TimeoutException if the lock cannot be acquired
     * @return enum indicating how the lock was acquired
     * @throws TimeoutException if the lock cannot be acquired before the timeout
     * @throws InterruptedException if the thread is interrupted while trying to acquire the lock
     */
    public LockResult lock(Serializable lockName, long timeout) throws TimeoutException, InterruptedException {
        return lock(lockName, timeout, false);
    }

    /**
     * Acquire the given lock.
     * @param lockName the identifier of the lock that should be acquired
     * @param timeout max time in ms to wait before throwing a TimeoutException if the lock cannot be acquired
     * @param newLock <code>true</code> if this object should assume this is the first use cluster-wide of the lock identified
     *        by <code>lockName</code>, and just acquire the lock locally without any cluster-wide call. See discussion of
     *        {@link LockResult#NEW_LOCK}.
     * @return enum indicating how the lock was acquired
     * @throws TimeoutException if the lock cannot be acquired before the timeout
     * @throws InterruptedException if the thread is interrupted while trying to acquire the lock
     */
    public LockResult lock(Serializable lockName, long timeout, boolean newLock) throws TimeoutException, InterruptedException {
        LockResult result = null;
        LocalLock localLock = getLocalLock(lockName, false);
        if (localLock == null) {
            localLock = getLocalLock(lockName, true);
            if (newLock) {
                // Here we assume the caller knows what they are doing and this
                // is really is a new lock, and that no other
                // node or local thread is trying to try to take it

                LockState lockState = localLock.lockForLocalNode();
                result = (lockState.localLockCount == 1 ? LockResult.NEW_LOCK : LockResult.ALREADY_HELD);
            }
        }

        if (result == null) { // We have to ask the cluster
            LockState lockState = localLock.registerForLocalLock();
            try {
                long remaining = timeout;
                do {
                    if (this.localNode == lockState.lockHolder) { // object identity is safe
                        result = LockResult.ALREADY_HELD;

                        // Check for race where we registered for something that's been removed
                        if (localLock.removable && localLock != getLocalLock(lockName, false)) {
                            // oops; try again
                            result = lock(lockName, remaining, newLock);
                        }
                    } else {
                        if (lockState.invalid) {
                            // the lock was removed; start over
                            result = lock(lockName, remaining, newLock);
                        } else if (lockState.localLockCount == 1) {
                            // Only one thread should ask the cluster for the lock;
                            // we were first so it's our task
                            if (this.clusterSupport.lock(lockName, remaining)) {
                                result = LockResult.ACQUIRED_FROM_CLUSTER;
                            } else {
                                throw new TimeoutException(MESSAGES.cannotAcquireLock(lockName));
                            }
                        } else {
                            // Some other thread is asking the cluster
                            // Wait for them to finish
                            long start = System.currentTimeMillis();
                            synchronized (localLock) {
                                lockState = localLock.lockState.get();
                                if (lockState.lockHolder != this.localNode) {
                                    localLock.wait(remaining);
                                }
                            }
                            // If something woke us up, see where we are
                            // and loop again
                            lockState = localLock.lockState.get();

                            remaining = timeout - (System.currentTimeMillis() - start);

                        }
                    }
                } while (result == null && remaining > 0);

                if (result == null) {
                    throwTimeoutException(lockName, lockState);
                }
            } finally {
                // Unregister interest in the lock

                // If we called clusterSupport.lock() above, its callback into
                // ClusterHandler.lockFromCluster() will increment the local lock count.
                // So, decrement so we don't double count
                // (If we threw an exception above we should also decrement)
                boolean decrement = (result != LockResult.ALREADY_HELD);

                // Only unregister if the current lock object for this key is
                // the same one we registered with above
                boolean updated = false;
                while (localLocks.get(lockName) == localLock && !updated) {
                    LockState currentState = localLock.lockState.get();
                    LockState unregistered = currentState.unregister(decrement);
                    updated = localLock.lockState.compareAndSet(currentState, unregistered);
                }
            }
        }

        return result;
    }

    /**
     * Releases a previously acquired lock.
     * @param lockName unique name identifying the lock to release
     * @param remove <code>true</code> if this lock can be removed from tracking once all local locks are unlocked.
     */
    public void unlock(Serializable lockName, boolean remove) {
        if (remove) {
            // Mark the lock removable so whatever thread reduces the local
            // lock count to zero can do the removal
            LocalLock lock = getLocalLock(lockName, false);
            if (lock != null) {
                lock.removable = true;
            }
        }

        this.clusterSupport.unlock(lockName);

        // See if it our responsibility to do a removal
        LocalLock lock = getLocalLock(lockName, false);
        if (lock != null && lock.removable && lock.lockState.get().lockHolder == null) {
            localLocks.remove(lockName, lock);
        }
    }

    /**
     * Brings this object to a state where it is ready for normal operation.
     * @throws Exception
     */
    public void start() throws Exception {
        this.clusterSupport.start();

        if (this.localNode == null) {
            throw MESSAGES.nullVar("localNode");
        }
    }

    /**
     * Removes this object from a state where it is ready for normal oepration and performs cleanup work.
     * @throws Exception
     */
    public void stop() throws Exception {
        this.clusterSupport.stop();
    }

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

    private static void throwTimeoutException(Serializable lockName, LockState lockState) throws TimeoutException {
        TimeoutException te = lockState.lockHolder == null ? new TimeoutException(MESSAGES.cannotAcquireLock(lockName))
                : new TimeoutException(lockState.lockHolder);
        throw te;
    }

}
