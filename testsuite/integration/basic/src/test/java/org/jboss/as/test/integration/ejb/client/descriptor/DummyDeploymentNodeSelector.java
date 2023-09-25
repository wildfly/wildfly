/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.client.descriptor;

import org.jboss.ejb.client.DeploymentNodeSelector;

/**
 * @author Jaikiran Pai
 */
public class DummyDeploymentNodeSelector implements DeploymentNodeSelector {
    @Override
    public String selectNode(String[] eligibleNodes, String appName, String moduleName, String distinctName) {
        return eligibleNodes[0];
    }
}
