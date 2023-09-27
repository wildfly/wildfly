/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.util.UUIDBuilder;
import org.wildfly.clustering.marshalling.protostream.util.UUIDMarshaller;

/**
 * {@link ProtoStreamMarshaller} for a session attribute map entry.
 * @author Paul Ferraro
 */
public enum SessionAttributeMapEntryMarshaller implements ProtoStreamMarshaller<Map.Entry<String, UUID>> {
    INSTANCE;

    private static final int ATTRIBUTE_NAME_INDEX = 1;
    private static final int ATTRIBUTE_ID_INDEX = 2;

    @Override
    public Map.Entry<String, UUID> readFrom(ProtoStreamReader reader) throws IOException {
        String attributeName = null;
        UUIDBuilder attributeIdBuilder = UUIDMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == ATTRIBUTE_NAME_INDEX) {
                attributeName = reader.readString();
            } else if (index >= ATTRIBUTE_ID_INDEX && index < ATTRIBUTE_ID_INDEX + UUIDMarshaller.INSTANCE.getFields()) {
                attributeIdBuilder = UUIDMarshaller.INSTANCE.readField(reader, index - ATTRIBUTE_ID_INDEX, attributeIdBuilder);
            } else {
                reader.skipField(tag);
            }
        }
        return new SessionAttributeMapEntry(attributeName, attributeIdBuilder.build());
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Map.Entry<String, UUID> entry) throws IOException {
        writer.writeString(ATTRIBUTE_NAME_INDEX, entry.getKey());
        UUIDMarshaller.INSTANCE.writeFields(writer, ATTRIBUTE_ID_INDEX, entry.getValue());
    }

    @Override
    public Class<? extends SessionAttributeMapEntry> getJavaClass() {
        return SessionAttributeMapEntry.class;
    }
}
