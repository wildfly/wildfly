/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.jgroups.spi;

import org.jgroups.Address;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Cache of physical addresses.
 * @author Paul Ferraro
 */
public enum PhysicalAddressCache implements Function<Address, PhysicalAddress>, ChannelListener {
    INSTANCE;

    private final Map<String, Function<Address, PhysicalAddress>> caches = new ConcurrentHashMap<>();

    @Override
    public PhysicalAddress apply(Address address) {
        return this.caches.values().stream().map(cache -> cache.apply(address)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Override
    public void channelConnected(JChannel channel) {
        this.caches.put(channel.getClusterName(), channel.getProtocolStack().getTransport()::getPhysicalAddressFromCache);
    }

    @Override
    public void channelDisconnected(JChannel channel) {
        this.caches.remove(channel.getClusterName());
    }
}
