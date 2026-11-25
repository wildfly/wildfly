/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import org.jgroups.Address;
import org.jgroups.util.NameCache;
import org.jgroups.util.UUID;
import org.wildfly.clustering.jgroups.spi.AddressFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * A default address factory.
 * @author Paul Ferraro
 */
public enum DefaultAddressFactory implements AddressFactory {
    INSTANCE;

    @Override
    public Address createAddress(String name, TransportConfiguration.Topology topology) {
        // Use secure random
        java.util.UUID id = java.util.UUID.randomUUID();
        UUID address = new UUID(id.getMostSignificantBits(), id.getLeastSignificantBits());
        NameCache.add(address, name);
        return address;
    }
}
