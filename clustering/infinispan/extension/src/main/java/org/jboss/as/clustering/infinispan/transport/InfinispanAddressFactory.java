/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.transport;

import java.util.UUID;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.NodeVersion;
import org.infinispan.remoting.transport.jgroups.AddressCache;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.NameCache;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.jgroups.spi.AddressFactory;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * Address factory for Infinispan.
 * @author Paul Ferraro
 */
@MetaInfServices(AddressFactory.class)
public class InfinispanAddressFactory implements AddressFactory {

    @Override
    public org.jgroups.Address createAddress(String name, TransportConfiguration.Topology topology) {
        // Use secure random
        UUID id = UUID.randomUUID();
        Address address = Address.protoFactory(id.getMostSignificantBits(), id.getLeastSignificantBits(), NodeVersion.INSTANCE, topology.getSite().orElse(null), topology.getRack().orElse(null), topology.getMachine().orElse(null));
        ExtendedUUID result = Address.toExtendedUUID(address);
        NameCache.add(result, name);
        AddressCache.fromExtendedUUID(result);
        return result;
    }
}
