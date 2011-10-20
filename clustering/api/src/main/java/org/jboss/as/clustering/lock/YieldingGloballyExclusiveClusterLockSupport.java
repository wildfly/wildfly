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

import static org.jboss.as.clustering.ClusteringApiLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 * Support class for cluster locking scenarios where threads cannot acquire a local lock unless the node owns a cluster-wide
 * lock, but where the node owning the cluster-wide lock will yield it to another node if no thread has a local lock. Use case
 * for this is scenarios like session management where a node needs to acquire ownership of a cluster-wide lock for a session,
 * but then once acquired wishes to handle multiple calls for the session without make cluster-wide locking calls. The node
 * handling the session would acquire the cluster-wide lock if it doesn't have it and thereafter would only use local locks; if
 * another node received a request for the session it would request the lock and the first node would release it if no local
 * locks are held.
 *
 * @author Brian Stansberry
 */
public class YieldingGloballyExclusiveClusterLockSupport extends AbstractClusterLockSupport {
    public YieldingGloballyExclusiveClusterLockSupport(String serviceHAName, GroupRpcDispatcher rpcDispatcher,
            GroupMembershipNotifier membershipNotifier, LocalLockHandler handler) {
        super(serviceHAName, rpcDispatcher, membershipNotifier, handler);
    }

    // ------------------------------------------------------ ClusterLockManager

    @Override
    public void unlock(Serializable lockId) {
        ClusterNode myself = getLocalClusterNode();
        if (myself == null) {
            throw MESSAGES.invalidMethodCall("start()", "unlock()");
        }

        ClusterLockState category = getClusterLockState(lockId, false);

        if (category == null) {
            getLocalHandler().unlockFromCluster(lockId, myself);
        } else if (myself.equals(category.getHolder())) {
            category.invalidate();
            getLocalHandler().unlockFromCluster(lockId, myself);
            removeLockState(category);
        }
    }

    // --------------------------------------------------------------- Protected

    @Override
    protected ClusterLockState getClusterLockState(Serializable categoryName) {
        return getClusterLockState(categoryName, false);
    }

    @Override
    protected RemoteLockResponse yieldLock(ClusterLockState lockState, ClusterNode caller, long timeout) {
        if (getLocalClusterNode().equals(lockState.getHolder())) {
            return getLock(lockState.lockId, lockState, caller, timeout);
        }
        return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.REJECT, lockState.getHolder());
    }

    @Override
    protected RemoteLockResponse handleLockSuccess(ClusterLockState lockState, ClusterNode caller) {
        if (getLocalClusterNode().equals(caller)) {
            recordLockHolder(lockState, caller);
        } else {
            // Caller succeeded, but since this node doesn't hold the
            // lock we don't want to hold the category in our map any longer
            lockState.invalidate();
            removeLockState(lockState);
        }
        return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.OK);
    }

    @Override
    protected RemoteLockResponse getRemoteLockResponseForUnknownLock(Serializable lockName, ClusterNode caller, long timeout) {
        RemoteLockResponse response;
        try {
            getLocalHandler().lockFromCluster(lockName, caller, timeout);
            return new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.OK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ROOT_LOGGER.caughtInterruptedException(caller, lockName);
            response = new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.FAIL, getLocalHandler()
                    .getLockHolder(lockName));
        } catch (TimeoutException t) {
            response = new RemoteLockResponse(getLocalClusterNode(), RemoteLockResponse.Flag.FAIL, t.getOwner());
        }
        return response;
    }
}
