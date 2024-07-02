/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.election;

import java.net.InetSocketAddress;

import org.wildfly.clustering.group.Node;

/**
 * A socket address preference.
 * @author Paul Ferraro
 * @deprecated To be removed without replacement.
 */
@Deprecated(forRemoval = true)
public class SocketAddressPreference implements Preference {
    private final InetSocketAddress address;

    public SocketAddressPreference(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public boolean preferred(Node node) {
        return node.getSocketAddress().getAddress().getHostAddress().equals(this.address.getAddress().getHostAddress()) && (node.getSocketAddress().getPort() == this.address.getPort());
    }

    @Override
    public String toString() {
        return this.address.toString();
    }
}
