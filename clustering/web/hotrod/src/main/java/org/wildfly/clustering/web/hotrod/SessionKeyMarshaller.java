/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.hotrod;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.OptionalInt;
import java.util.function.Function;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.wildfly.clustering.infinispan.client.Key;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataInput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamDataOutput;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.spi.Serializer;
import org.wildfly.clustering.web.cache.SessionIdentifierSerializer;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * @author Paul Ferraro
 */
public class SessionKeyMarshaller<K extends Key<String>> implements ProtoStreamMarshaller<K> {
    private static final Serializer<String> IDENTIFIER_SERIALIZER = SessionIdentifierSerializer.INSTANCE;

    private final Class<K> targetClass;
    private final Function<String, K> resolver;

    public SessionKeyMarshaller(Class<K> targetClass, Function<String, K> resolver) {
        this.targetClass = targetClass;
        this.resolver = resolver;
    }

    @Override
    public K readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        String id = IDENTIFIER_SERIALIZER.read(new ProtoStreamDataInput(reader));
        if (reader.readTag() != 0) {
            throw new StreamCorruptedException();
        }
        return this.resolver.apply(id);
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, K key) throws IOException {
        String id = key.getId();
        IDENTIFIER_SERIALIZER.write(new ProtoStreamDataOutput(writer), id);
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, K value) {
        // Use conservative estimate
        return OptionalInt.of(CodedOutputStream.computeStringSizeNoTag(value.getId()));
    }

    @Override
    public Class<? extends K> getJavaClass() {
        return this.targetClass;
    }
}
