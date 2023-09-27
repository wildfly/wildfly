/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.client.cluster;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Remote(NodeNameEcho.class)
public class ClusteredStatefulNodeNameEcho implements NodeNameEcho {
    @Override
    public String getNodeName(boolean preferOtherNode) {
        return System.getProperty("jboss.node.name");
    }
}
