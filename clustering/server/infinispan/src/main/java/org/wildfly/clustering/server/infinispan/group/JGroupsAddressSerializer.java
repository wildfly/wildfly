/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.Address;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.FunctionalSerializer;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * Serializer for an Infinispan JGroups-based address.
 * @author Paul Ferraro
 */
public class JGroupsAddressSerializer extends FunctionalSerializer<JGroupsAddress, Address> {
    static final Serializer<JGroupsAddress> INSTANCE = new JGroupsAddressSerializer();

    private JGroupsAddressSerializer() {
        super(AddressSerializer.INSTANCE, JGroupsAddress::getJGroupsAddress, JGroupsAddress::new);
    }

    @MetaInfServices(Externalizer.class)
    public static class JGroupsAddressExternalizer extends SerializerExternalizer<JGroupsAddress> {
        public JGroupsAddressExternalizer() {
            super(JGroupsAddress.class, INSTANCE);
        }
    }

    @MetaInfServices(Formatter.class)
    public static class JGroupsAddressFormatter extends BinaryFormatter<JGroupsAddress> {
        public JGroupsAddressFormatter() {
            super(JGroupsAddress.class, INSTANCE);
        }
    }
}
