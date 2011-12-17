package org.jboss.as.clustering.singleton.election;

import java.net.InetSocketAddress;

import org.jboss.as.clustering.ClusterNode;

public class SocketAddressPreference implements Preference {
    private final InetSocketAddress address;

    public SocketAddressPreference(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public boolean preferred(ClusterNode node) {
        return node.getIpAddress().getHostAddress().equals(this.address.getAddress().getHostAddress()) && (node.getPort() == this.address.getPort());
    }
}
