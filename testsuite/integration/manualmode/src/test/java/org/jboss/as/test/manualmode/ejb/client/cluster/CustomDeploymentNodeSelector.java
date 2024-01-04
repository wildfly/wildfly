/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.cluster;

import org.jboss.ejb.client.DeploymentNodeSelector;

/**
 * @author Jaikiran Pai
 */
public class CustomDeploymentNodeSelector implements DeploymentNodeSelector {

    private volatile String previouslySelectedNode;

    @Override
    public String selectNode(String[] eligibleNodes, String appName, String moduleName, String distinctName) {
        if (eligibleNodes.length == 1) {
            return eligibleNodes[0];
        }
        for (String node : eligibleNodes) {
            if (!node.equals(previouslySelectedNode)) {
                this.previouslySelectedNode = node;
                return node;
            }
        }
        return eligibleNodes[0];
    }
}
