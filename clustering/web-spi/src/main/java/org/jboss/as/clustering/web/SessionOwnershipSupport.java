/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web;

import java.util.concurrent.TimeoutException;

/**
 * @author Paul Ferraro
 */
public interface SessionOwnershipSupport {
    /** Result of a {@link #acquireSessionOwnership(String, boolean) lock call} */
    public static enum LockResult {
        /** Indicates the lock was acquired after requesting it from the cluster */
        ACQUIRED_FROM_CLUSTER,
        /** Indicates this node already held the lock */
        ALREADY_HELD,
        /**
         * Indicates the 'newLock' param passed to {@link #lock(String, boolean)} was <code>true</code> and the local node in
         * fact was unaware of the lock. If in fact the local node was already aware of the lock (which would generally indicate
         * a flaw in the application using this class) NEW_LOCK will not be returned; rather one of the other enum values will
         * be returned.
         */
        NEW_LOCK,
        /** Indicates {@link #getSupportsSessionOwnership()} will return <code>false</code> */
        UNSUPPORTED
    }

    /**
     * Attempt to take ownership of the session identified by the given id.
     * @param realId the session's id, excluding any jvmRoute
     * @param newLock <code>true</code> if the caller knows this is a new session that doesn't exist elsewhere in the cluster,
     *        and thus acquiring ownership does not require any cluster-wide call
     * @return the result of the attempt
     * @throws TimeoutException
     * @throws InterruptedException
     */
    LockResult acquireSessionOwnership(String realId, boolean newLock) throws TimeoutException, InterruptedException;

    /**
     * Releases ownership of a session, thus making it possible for other nodes to acquire it.
     * @param realId the session's id, excluding any jvmRoute
     * @param remove <code>true</code> if this release is associated with a session removal, in which case the distributed cache
     *        manager can stop tracking ownership
     */
    void relinquishSessionOwnership(String realId, boolean remove);
}
