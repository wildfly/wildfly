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
 * Return value for a "remoteLock" call. This class is public as an aid in unit testing.
 */
public class RemoteLockResponse implements Serializable {
    public enum Flag {
        /** Lock acquired on responding node */
        OK,
        /** Attempt to acquire local lock failed */
        FAIL,
        /**
         * Request rejected either because lock is held or local node is attempting to acquire lock.
         */
        REJECT
    }

    /** The serialVersionUID */
    private static final long serialVersionUID = -8878607946010425555L;

    public final RemoteLockResponse.Flag flag;
    public final ClusterNode holder;
    public final ClusterNode responder;

    public RemoteLockResponse(ClusterNode responder, RemoteLockResponse.Flag flag) {
        this(responder, flag, null);
    }

    public RemoteLockResponse(ClusterNode responder, RemoteLockResponse.Flag flag, ClusterNode holder) {
        this.flag = flag;
        this.holder = holder;
        this.responder = responder;
    }
}