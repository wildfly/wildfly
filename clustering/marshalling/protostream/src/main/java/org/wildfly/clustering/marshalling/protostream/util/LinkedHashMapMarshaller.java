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

package org.wildfly.clustering.marshalling.protostream.util;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.AbstractMap.SimpleEntry;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamReader;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.spi.util.LinkedHashMapExternalizer;

import protostream.com.google.protobuf.CodedOutputStream;

/**
 * Marshaller for a {@link LinkedHashMap}.
 * @author Paul Ferraro
 */
public class LinkedHashMapMarshaller extends AbstractMapMarshaller<LinkedHashMap<Object, Object>> {

    private static final int ACCESS_ORDER_INDEX = 2;

    @SuppressWarnings("unchecked")
    public LinkedHashMapMarshaller() {
        super((Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class);
    }

    @Override
    public LinkedHashMap<Object, Object> readFrom(ImmutableSerializationContext context, RawProtoStreamReader reader) throws IOException {
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>(16, 0.75f, false);
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            switch (index) {
                case ENTRY_INDEX:
                    Map.Entry<Object, Object> entry = ProtoStreamMarshaller.read(context, reader.readByteBuffer(), SimpleEntry.class);
                    map.put(entry.getKey(), entry.getValue());
                    break;
                case ACCESS_ORDER_INDEX:
                    LinkedHashMap<Object, Object> existing = map;
                    map = new LinkedHashMap<>(16, 0.75f, reader.readBool());
                    map.putAll(existing);
                    break;
                default:
                    reading = (tag != 0) && reader.skipField(tag);
            }
        }
        return map;
    }

    @Override
    public void writeTo(ImmutableSerializationContext context, RawProtoStreamWriter writer, LinkedHashMap<Object, Object> map) throws IOException {
        super.writeTo(context, writer, map);
        boolean accessOrder = LinkedHashMapExternalizer.ACCESS_ORDER.apply(map);
        if (accessOrder) {
            writer.writeBool(ACCESS_ORDER_INDEX, accessOrder);
        }
    }

    @Override
    public OptionalInt size(ImmutableSerializationContext context, LinkedHashMap<Object, Object> map) {
        int size = 0;
        OptionalInt entriesSize = super.size(context, map);
        if (entriesSize.isPresent()) {
            size += entriesSize.getAsInt();
        } else {
            return entriesSize;
        }
        boolean accessOrder = LinkedHashMapExternalizer.ACCESS_ORDER.apply(map);
        if (accessOrder) {
            size += CodedOutputStream.computeBoolSize(ACCESS_ORDER_INDEX, accessOrder);
        }
        return OptionalInt.of(size);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends LinkedHashMap<Object, Object>> getJavaClass() {
        return (Class<LinkedHashMap<Object, Object>>) (Class<?>) LinkedHashMap.class;
    }
}
