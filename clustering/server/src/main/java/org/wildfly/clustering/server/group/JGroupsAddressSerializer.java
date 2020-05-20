/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.server.group;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.infinispan.spi.persistence.BinaryKeyFormat;
import org.wildfly.clustering.infinispan.spi.persistence.KeyFormat;
import org.wildfly.clustering.marshalling.Externalizer;
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

    @MetaInfServices(KeyFormat.class)
    public static class JGroupsAddressKeyFormat extends BinaryKeyFormat<JGroupsAddress> {
        public JGroupsAddressKeyFormat() {
            super(JGroupsAddress.class, INSTANCE);
        }
    }
}
