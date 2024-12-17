/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.group;

import java.net.InetSocketAddress;

/**
 * Identifies a member of a cluster.
 *
 * @author Paul Ferraro
 * @deprecated Replaced by {@link org.wildfly.clustering.server.GroupMember}.
 */
@Deprecated(forRemoval = true)
public interface Node {
    /**
     * Returns the logical name of this node.
     *
     * @return a unique name
     */
    String getName();

    /**
     * Returns the unique socking binding address of this node.
     *
     * @return a socket binding address, or null if this node is a member of a singleton group.
     */
    default InetSocketAddress getSocketAddress() {
        return null;
    }
}
