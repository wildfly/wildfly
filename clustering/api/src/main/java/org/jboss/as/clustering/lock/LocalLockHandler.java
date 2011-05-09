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

/**
 * Provides local lock coordination for an {@link AbstractClusterLockSupport}.
 *
 * @author Brian Stansberry
 *
 * @version $Revision: 86130 $
 */
public interface LocalLockHandler {
    /**
     * Gets the node the holds the given lock on this node, or <code>null</code> if no node holds the lock on this node.
     *
     * @param lockName
     * @return
     */
    ClusterNode getLockHolder(Serializable lockName);

    /**
     * Try to acquire the local lock within the given timeout. If this method returns successfully, the caller has acquired the
     * lock.
     *
     * @param lockName the name of the lock.
     * @param caller the node making the request
     * @param timeout number of ms the caller will accept waiting before the lock acquisition should be considered a failure. A
     *        value less than one means wait as long as necessary.
     *
     * @throws TimeoutException if the lock could not be acquired within the specified timeout
     * @throws InterruptedException
     */
    void lockFromCluster(Serializable lockName, ClusterNode caller, long timeout) throws TimeoutException, InterruptedException;

    /**
     * Release the lock.
     *
     * @param lockName the name of the lock.
     * @param caller the node making the request
     */
    void unlockFromCluster(Serializable lockName, ClusterNode caller);

    ClusterNode getLocalNode(ClusterNode localNode);

    void setLocalNode(ClusterNode localNode);
}
