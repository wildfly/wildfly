/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.notification;

import static org.jboss.as.web.WebMessages.MESSAGES;

/**
 * Encapsulates the status of how the local container is managing the given session.
 * @author Brian Stansberry
 */
public class ClusteredSessionManagementStatus {
    private final String realId;
    private final boolean locallyUsed;
    private final Boolean locallyActive;
    private final Boolean locallyOwned;

    /**
     * Create a new ClusteredSessionManagementStatus.
     * @param realId the id of the session, excluding any jvmRoute.
     * @param locallyUsed whether the session has been provided to the application on this node.
     * @param locallyActive whether this node is the most recent one to handle a request for the session; <code>null</code> if unknown
     * @param locallyOwned whether this node is the "owner" of the session, <code>null</code> if unknown or the concept of ownership is unsupported.
     */
    public ClusteredSessionManagementStatus(String realId, boolean locallyUsed, Boolean locallyActive, Boolean locallyOwned) {
        if (realId == null) {
            throw MESSAGES.nullRealId();
        }
        this.realId = realId;
        this.locallyUsed = locallyUsed;
        // If we haven't been locallyUsed, we can't be locallyActive
        this.locallyActive = (locallyUsed ? locallyActive : Boolean.FALSE);
        // If we are locallyActive, we are locally owned
        this.locallyOwned = (Boolean.TRUE.equals(locallyActive) ? Boolean.TRUE : locallyOwned);
    }

    /**
     * Gets the id of the session, excluding any jvmRoute that may have been appended if JK is used.
     * @return the id. Will not return <code>null</code>.
     */
    public String getRealId() {
        return realId;
    }

    /**
     * Gets whether an HttpSession object for the given session has been returned from the container to the application on this node.
     * @return <code>true</code> if the session has been used locally, <code>false</code> if not.
     */
    public boolean isLocallyUsed() {
        return locallyUsed;
    }

    /**
     * Gets whether an HttpSession object for the given session has been returned from the container to the application on this
     * node AND this node is the last one to handle a request for the session.
     * @return <code>true</code> if the above conditions are true and the container is sure of this, <code>false</code> if they
     *         are not true and the container knows this, or <code>null</code> if the container is unsure if this node is the
     *         last one to handle a request for the session.
     * @see ClusteredSessionNotificationCapability#isLocallyActiveAware()
     */
    public Boolean getLocallyActive() {
        return locallyActive;
    }

    /**
     * Gets whether this node considers itself to be the "owner" of the session; i.e. the one primarily responsible for managing
     * its lifecycle. Note that a node that is undeploying a war will always give up ownership of its sessions if it is aware of
     * other nodes in the cluster that still have the war deployed.
     * @return <code>true</code> if the container knows it is the owner, <code>false</code> if it knows it is not the owner, or
     *         <code>null</code> if the container is unsure about ownership or does not recognize the concept of ownership.
     * @see ClusteredSessionNotificationCapability#isLocallyOwnedAware()
     */
    public Boolean getLocallyOwned() {
        return locallyOwned;
    }
}
