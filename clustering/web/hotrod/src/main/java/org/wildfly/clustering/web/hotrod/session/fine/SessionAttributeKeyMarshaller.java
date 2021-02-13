/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod.session.fine;

import java.io.IOException;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.util.UUIDBuilder;
import org.wildfly.clustering.marshalling.protostream.util.UUIDMarshaller;
import org.wildfly.clustering.web.cache.SessionIdentifierMarshaller;

/**
 * @author Paul Ferraro
 */
public class SessionAttributeKeyMarshaller implements ProtoStreamMarshaller<SessionAttributeKey> {

    private static final int SESSION_IDENTIFIER_INDEX = 1;
    private static final int ATTRIBUTE_IDENTIFIER_INDEX = 2;

    @Override
    public SessionAttributeKey readFrom(ProtoStreamReader reader) throws IOException {
        String sessionId = null;
        UUIDBuilder attributeId = UUIDMarshaller.INSTANCE.getBuilder();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if (index == SESSION_IDENTIFIER_INDEX) {
                sessionId = SessionIdentifierMarshaller.INSTANCE.readFrom(reader);
            } else if (index >= ATTRIBUTE_IDENTIFIER_INDEX && index < ATTRIBUTE_IDENTIFIER_INDEX + UUIDMarshaller.INSTANCE.getFields()) {
                attributeId = UUIDMarshaller.INSTANCE.readField(reader, index - ATTRIBUTE_IDENTIFIER_INDEX, attributeId);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return new SessionAttributeKey(sessionId, attributeId.build());
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