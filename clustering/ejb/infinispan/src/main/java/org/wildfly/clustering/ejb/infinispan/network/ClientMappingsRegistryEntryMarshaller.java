/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.protostream.descriptors.WireType;
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
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case MEMBER_INDEX:
                    memberName = reader.readString();
                    break;
                case CLIENT_MAPPING_INDEX:
                    mappings.add(reader.readObject(ClientMapping.class));
                    break;
                default:
                    reader.skipField(tag);
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
