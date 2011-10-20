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

import org.jboss.as.clustering.ClusterNode;

import static org.jboss.as.clustering.ClusteringApiMessages.MESSAGES;

/**
 * Thrown to indicate failure to acquire a lock within a specified timeout.
 *
 * @author Brian Stansberry
 */
public class TimeoutException extends java.util.concurrent.TimeoutException {
    /** The serialVersionUID */
    private static final long serialVersionUID = 7786468949269669386L;

    private ClusterNode owner;

    public TimeoutException(ClusterNode owner) {
        super((owner == null ? MESSAGES.cannotAcquireHeldLock() : MESSAGES.cannotAcquireHeldLock(owner)));
        this.owner = owner;
    }

    public TimeoutException(String msg) {
        super(msg);
    }

    public ClusterNode getOwner() {
        return owner;
    }
}
