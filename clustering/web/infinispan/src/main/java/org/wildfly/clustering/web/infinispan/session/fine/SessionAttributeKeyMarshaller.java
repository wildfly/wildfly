/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.fine;

import java.io.IOException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.util.UUIDBuilder;
import org.wildfly.clustering.marshalling.protostream.util.UUIDMarshaller;
import org.wildfly.clustering.web.cache.SessionIdentifierMarshaller;

public class SessionAttributeKeyMarshaller implements ProtoStreamMarshaller<SessionAttributeKey> {

    private static final int SESSION_IDENTIFIER_INDEX = 1;
    private static final int ATTRIBUTE_IDENTIFIER_INDEX = 2;

    @Override
    public SessionAttributeKey readFrom(ProtoStreamReader reader) throws IOException {
        String sessionId = null;
        UUIDBuilder attributeIdBuilder = UUIDMarshaller.INSTANCE.getBuilder();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (index == SESSION_IDENTIFIER_INDEX) {
                sessionId = SessionIdentifierMarshaller.INSTANCE.readFrom(reader);
            } else if (index >= ATTRIBUTE_IDENTIFIER_INDEX && index < ATTRIBUTE_IDENTIFIER_INDEX + UUIDMarshaller.INSTANCE.getFields()) {
                UUIDMarshaller.INSTANCE.readField(reader, index - ATTRIBUTE_IDENTIFIER_INDEX, attributeIdBuilder);
            } else {
                reader.skipField(tag);
            }
        }
        return new SessionAttributeKey(sessionId, attributeIdBuilder.build());
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SessionAttributeKey key) throws IOException {
        writer.writeTag(SESSION_IDENTIFIER_INDEX, SessionIdentifierMarshaller.INSTANCE.getWireType());
        SessionIdentifierMarshaller.INSTANCE.writeTo(writer, key.getId());
        UUIDMarshaller.INSTANCE.writeFields(writer, ATTRIBUTE_IDENTIFIER_INDEX, key.getAttributeId());
    }

    @Override
    public Class<? extends SessionAttributeKey> getJavaClass() {
        return SessionAttributeKey.class;
    }
}