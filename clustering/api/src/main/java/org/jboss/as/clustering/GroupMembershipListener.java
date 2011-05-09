/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc. and individual contributors
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

package org.jboss.as.clustering;

import java.util.List;

/**
 * Listener for notifications issued when a new node joins the cluster or an existing node leaves the cluster (or simply dies).
 *
 * @author Brian Stansberry
 *
 * @version $Revision$
 */
public interface GroupMembershipListener {
    /**
     * Called when a partition topology change occurs. This callback will not be made using the thread that carries messages up
     * from the network.
     *
     * @param deadMembers A list of nodes that have died since the previous view
     * @param newMembers A list of nodes that have joined the partition since the previous view
     * @param allMembers A list of nodes that built the current view
     */
    void membershipChanged(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers);

    /**
     * Specialized notification issued instead of {@link #membershipChanged(List, List, List) the standard one} when a
     * network-partition merge occurs. This callback will not be made using the thread that carries messages up from the
     * network.
     *
     * @param deadMembers A list of nodes that have died since the previous view
     * @param newMembers A list of nodes that have joined the partition since the previous view
     * @param allMembers A list of nodes that built the current view
     * @param originatingGroups A list of nodes that were previously partioned and that are now merged
     */
    void membershipChangedDuringMerge(List<ClusterNode> deadMembers, List<ClusterNode> newMembers, List<ClusterNode> allMembers, List<List<ClusterNode>> originatingGroups);
}
