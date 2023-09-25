/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton;

import java.util.Set;

import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public interface Singleton {

    /**
     * Indicates whether this node is the primary provider of the singleton.
     * @return true, if this node is the primary node, false if it is a backup node.
     */
    boolean isPrimary();

    /**
     * Returns the current primary provider of the singleton.
     * @return a cluster member
     */
    Node getPrimaryProvider();

    /**
     * Returns the providers on which the given singleton is available.
     * @return a set of cluster members
     */
    Set<Node> getProviders();
}
