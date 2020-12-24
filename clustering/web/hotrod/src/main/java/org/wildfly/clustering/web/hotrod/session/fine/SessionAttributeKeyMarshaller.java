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
import java.util.OptionalInt;
import java.util.UUID;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataInput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataOutput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.util.UUIDMarshaller;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.web.cache.SessionIdentifierSerializer;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public enum SessionAttributeKeyMarshaller implements ProtoStreamMarshaller<SessionAttributeKey> {
    INSTANCE;

    private static final Serializer<String> IDENTIFIER_SERIALIZER = SessionIdentifierSerializer.INSTANCE;

    @Override
    public SessionAttributeKey readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        String sessionId = IDENTIFIER_SERIALIZER.read(new ProtoStreamDataInput(reader));
        UUID attributeId = UUIDMarshaller.INSTANCE.readFrom(context, reader);
        return new SessionAttributeKey(sessionId, attributeId);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, SessionAttributeKey key) throws IOException {
        String sessionId = key.getId();
        UUID attributeId = key.getAttributeId();
        IDENTIFIER_SERIALIZER.write(new ProtoStreamDataOutput(writer), sessionId);
        UUIDMarshaller.INSTANCE.writeTo(context, writer, attributeId);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, SessionAttributeKey value) {
        OptionalInt size = UUIDMarshaller.INSTANCE.size(context, value.getAttributeId());
        return size.isPresent() ? OptionalInt.of(CodedOutputStream.computeStringSizeNoTag(value.getId()) + size.getAsInt()) : size;
    }

    @Override
    public Class<? extends SessionAttributeKey> getJavaClass() {
        return SessionAttributeKey.class;
    }
}