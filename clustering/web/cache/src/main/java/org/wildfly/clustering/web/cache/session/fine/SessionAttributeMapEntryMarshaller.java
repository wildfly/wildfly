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

package org.wildfly.clustering.web.cache.session.fine;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.infinispan.protostream.impl.WireFormat;
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
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index == ATTRIBUTE_NAME_INDEX) {
                attributeName = reader.readString();
            } else if (index >= ATTRIBUTE_ID_INDEX && index < ATTRIBUTE_ID_INDEX + UUIDMarshaller.INSTANCE.getFields()) {
                attributeIdBuilder = UUIDMarshaller.INSTANCE.readField(reader, index - ATTRIBUTE_ID_INDEX, attributeIdBuilder);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
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
