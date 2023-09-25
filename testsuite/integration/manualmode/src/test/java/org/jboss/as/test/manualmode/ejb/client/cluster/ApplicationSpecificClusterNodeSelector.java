/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
