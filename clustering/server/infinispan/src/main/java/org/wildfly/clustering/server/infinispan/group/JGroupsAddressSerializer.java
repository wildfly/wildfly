/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.BinaryFormatter;
import org.wildfly.clustering.marshalling.spi.Formatter;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.marshalling.spi.SerializerExternalizer;

/**
 * Serializer for an Infinispan JGroups-based address.
 * @author Paul Ferraro
 */
public enum JGroupsAddressSerializer implements Serializer<JGroupsAddress> {
    INSTANCE;

    @Override
    public void write(DataOutput output, JGroupsAddress address) throws IOException {
        AddressSerializer.INSTANCE.write(output, address.getJGroupsAddress());
    }

    @Override
    public JGroupsAddress read(DataInput input) throws IOException {
        return new JGroupsAddress(AddressSerializer.INSTANCE.read(input));
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
