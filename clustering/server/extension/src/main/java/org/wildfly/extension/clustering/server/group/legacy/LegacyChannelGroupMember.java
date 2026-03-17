/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.jgroups.Address;
import org.jgroups.PhysicalAddress;
import org.wildfly.clustering.jgroups.spi.PhysicalAddressCache;
import org.wildfly.clustering.server.jgroups.ChannelGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyChannelGroupMember extends LegacyGroupMember<Address> {

    @Override
    ChannelGroupMember unwrap();

    @Override
    default InetSocketAddress getSocketAddress() {
        return Optional.ofNullable(PhysicalAddressCache.INSTANCE.apply(this.unwrap().getId()))
                .map(PhysicalAddress::getSocketAddress)
                .filter(InetSocketAddress.class::isInstance)
                .map(InetSocketAddress.class::cast)
                .orElse(null);
    }

    static LegacyChannelGroupMember wrap(ChannelGroupMember member) {
        return () -> member;
    }
}
