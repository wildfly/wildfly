/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

/**
 * @author Ondrej Chaloupka
 */
public class NodeNameGetter {

    private NodeNameGetter() {
    }

    public static String getNodeName() {
        return System.getProperty("jboss.node.name");
    }
}
