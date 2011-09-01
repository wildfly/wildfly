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

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupMembershipNotifier;
import org.jboss.as.clustering.GroupRpcDispatcher;

import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 * Support class for cluster locking scenarios where threads can hold a local lock on a category but not a cluster-wide lock.
 * Multiple nodes can simultaneously hold a local lock on a category, but none can hold a local lock on a category if the
 * cluster-wide lock is held. Cluster-wide lock cannot be acquired while any node holds a local lock.
 * <p>
 * <strong>NOTE:</strong> This class does not support "upgrades", i.e. scenarios where a thread acquires the local lock and then
 * while holding the local lock attempts to acquire the cluster-wide lock.
 * </p>
 *
 * @author Brian Stansberry
 */
public class NonGloballyExclusiveClusterLockSupport extends AbstractClusterLockSupport {

    // ------------------------------------------------------------- Constructor

    public NonGloballyExclusiveClusterLockSupport(String serviceHAName, GroupRpcDispatcher rpcDispatcher,
            GroupMembershipNotifier membershipNotifier, LocalLockHandler handler) {
        super(serviceHAName, rpcDispatcher, membershipNotifier, handler);
    }

    // ------------------------------------------------------------------ Public

    @Override
    public void unlock(Serializable lockId) {
        ClusterNode myself = getLocalClusterNode();
        if (myself == null) {
            throw MESSAGES.invalidMethodCall("start()", "unlock()");
        }

        ClusterLockState lockState = getClusterLockState(lockId, false);

        if (lockState != null && myself.equals(lockState.getHolder())) {
            getLocalHandler().unlockFromCluster(lockId, myself);
            lockState.release();

            try {
                getGroupRpcDispatcher().callMethodOnCluster(getServiceHAName(), "releaseRemoteLock",
                        new Object[] { lockId, myself }, RELEASE_REMOTE_LOCK_TYPES, true);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw MESSAGES.remoteLockReleaseFailure(e);
            }
        }
    }

    // --------------------------------------------------------------- Protected

    @Override
    protected ClusterLockState getClusterLockState(Serializable categoryName) {
        return getClusterLockState(categoryName, true);
    }

    @Override
    protected RemoteLockResponse yieldLock(ClusterLockState lockState, ClusterNode caller, long timeout) {
        return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.REJECT, lockState.getHolder());
    }

    @Override
    protected RemoteLockResponse handleLockSuccess(ClusterLockState lockState, ClusterNode caller) {
        recordLockHolder(lockState, caller);
        return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.OK);
    }

    @Override
    protected RemoteLockResponse getRemoteLockResponseForUnknownLock(Serializable lockName, ClusterNode caller, long timeout) {
        // unknown == OK
        return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.OK);
    }

    // ----------------------------------------------------------------- Private
}
