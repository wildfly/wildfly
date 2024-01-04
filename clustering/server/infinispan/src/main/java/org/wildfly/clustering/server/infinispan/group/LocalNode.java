/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.group;

import java.net.InetSocketAddress;

import org.wildfly.clustering.group.Node;

/**
 * Non-clustered {@link Node} implementation.
 * @author Paul Ferraro
 */
public class LocalNode implements Node, Comparable<LocalNode> {

    private final String name;

    public LocalNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return null;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof LocalNode)) return false;
        LocalNode node = (LocalNode) object;
        return this.name.equals(node.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(LocalNode node) {
        return this.name.compareTo(node.name);
    }
}
