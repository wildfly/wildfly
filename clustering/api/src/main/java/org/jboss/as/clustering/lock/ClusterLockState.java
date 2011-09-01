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
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.ClusterNode;

import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 *
 * @author Brian Stansberry
 *
 * @version $Revision:$
 */
public class ClusterLockState {
    /** Lock status of a category */
    public enum State {
        /** No one has the lock and local node is not attempting to acquire */
        UNLOCKED,
        /** Local node is attempting to acquire lock across cluster */
        REMOTE_LOCKING,
        /** Local node has lock across cluster, now attempting locally */
        LOCAL_LOCKING,
        /** The lock is held locally */
        LOCKED,
        /**
         * This object has been removed from the categories map and should be discarded
         */
        INVALID
    }

    final Serializable lockId;
    final AtomicReference<ClusterLockState.State> state = new AtomicReference<ClusterLockState.State>(State.UNLOCKED);
    ClusterNode holder;

    ClusterLockState(Serializable lockId) {
        if (lockId == null) {
            throw MESSAGES.nullVar("lockId");
        }
        this.lockId = lockId;
    }

    public synchronized ClusterNode getHolder() {
        return holder;
    }

    public synchronized void invalidate() {
        this.state.set(State.INVALID);
        this.holder = null;
    }

    public synchronized void lock(ClusterNode holder) {
        this.state.set(State.LOCKED);
        this.holder = holder;
    }

    public synchronized void release() {
        if (this.state.compareAndSet(State.LOCKED, State.UNLOCKED)) {
            this.holder = null;
        }
    }
}