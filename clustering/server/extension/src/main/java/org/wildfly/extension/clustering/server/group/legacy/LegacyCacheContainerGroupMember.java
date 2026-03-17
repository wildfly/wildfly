/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import java.net.InetSocketAddress;
import java.util.Optional;

import org.infinispan.remoting.transport.Address;
import org.jgroups.PhysicalAddress;
import org.wildfly.clustering.jgroups.spi.PhysicalAddressCache;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheContainerGroupMember extends LegacyGroupMember<Address> {

    @Override
    CacheContainerGroupMember unwrap();

    @Override
    default InetSocketAddress getSocketAddress() {
        return Optional.ofNullable(this.unwrap().getId())
                .map(Address::toExtendedUUID)
                .map(PhysicalAddressCache.INSTANCE)
                .map(PhysicalAddress::getSocketAddress)
                .filter(InetSocketAddress.class::isInstance)
                .map(InetSocketAddress.class::cast)
                .orElse(null);
    }

    static LegacyCacheContainerGroupMember wrap(CacheContainerGroupMember member) {
        return () -> member;
    }
}
