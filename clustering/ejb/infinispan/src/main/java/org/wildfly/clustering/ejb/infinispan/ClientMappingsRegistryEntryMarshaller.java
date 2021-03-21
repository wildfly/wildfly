/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.infinispan;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.impl.WireFormat;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * {@link ProtoStreamMarshaller} for a {@link ClientMappingsRegistryEntry}.
 * @author Paul Ferraro
 */
public class ClientMappingsRegistryEntryMarshaller implements ProtoStreamMarshaller<ClientMappingsRegistryEntry> {

    private static final int MEMBER_INDEX = 1;
    private static final int CLIENT_MAPPING_INDEX = 2;

    @Override
    public ClientMappingsRegistryEntry readFrom(ProtoStreamReader reader) throws IOException {
        String memberName = null;
        List<ClientMapping> mappings = new LinkedList<>();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            switch (WireFormat.getTagFieldNumber(tag)) {
                case MEMBER_INDEX:
                    memberName = reader.readString();
                    break;
                case CLIENT_MAPPING_INDEX:
                    mappings.add(reader.readObject(ClientMapping.class));
                    break;
                default:
                    reading = reader.ignoreField(tag);
            }
        }
        return new ClientMappingsRegistryEntry(memberName, mappings);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, ClientMappingsRegistryEntry entry) throws IOException {
        writer.writeString(MEMBER_INDEX, entry.getKey());
        for (ClientMapping mapping : entry.getValue()) {
            writer.writeObject(CLIENT_MAPPING_INDEX, mapping);
        }
    }

    @Override
    public Class<? extends ClientMappingsRegistryEntry> getJavaClass() {
        return ClientMappingsRegistryEntry.class;
    }
}
