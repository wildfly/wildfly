/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.cluster;

import org.jboss.ejb.client.ClusterNodeSelector;

import java.util.Random;

/**
 * @author Jaikiran Pai
 */
public class ApplicationSpecificClusterNodeSelector implements ClusterNodeSelector {
    @Override
    public String selectNode(final String clusterName, final String[] connectedNodes, final String[] availableNodes) {
        final Random random = new Random();
        // check if there are any connected nodes. If there are then just reuse them
        if (connectedNodes.length > 0) {
            if (connectedNodes.length == 1) {
                return connectedNodes[0];
            }
            final int randomConnectedNode = random.nextInt(connectedNodes.length);
            return connectedNodes[randomConnectedNode];
        }
        // there are no connected nodes. so use the available nodes and let the cluster context
        // establish a connection for the selected node
        if (availableNodes.length == 1) {
            return availableNodes[0];
        }
        final int randomSelection = random.nextInt(availableNodes.length);
        return availableNodes[randomSelection];
    }
}
